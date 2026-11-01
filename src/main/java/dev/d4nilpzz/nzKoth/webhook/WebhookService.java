package dev.d4nilpzz.nzKoth.webhook;

import dev.d4nilpzz.nzKoth.NzKoth;
import dev.d4nilpzz.nzKoth.koth.Koth;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

public final class WebhookService {

    private final NzKoth plugin;
    private final Map<String, String> templates = new HashMap<>();
    private HttpClient client;
    private String url = "";
    private String username = "NzKoth";
    private int color = 0xFF4D4D;
    private boolean enabled;

    public WebhookService(@NotNull NzKoth plugin) {
        this.plugin = plugin;
    }

    public void load() {
        templates.clear();
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("webhooks");
        enabled = section != null && section.getBoolean("enabled", false);
        if (!enabled) {
            return;
        }
        url = section.getString("url", "");
        username = section.getString("username", "NzKoth");
        color = section.getInt("color", 0xFF4D4D);

        ConfigurationSection events = section.getConfigurationSection("events");
        if (events != null) {
            for (String key : events.getKeys(false)) {
                String template = events.getString(key, "");
                if (!template.isBlank()) {
                    templates.put(key.toLowerCase(java.util.Locale.ROOT), template);
                }
            }
        }
        if (url.isBlank()) {
            enabled = false;
            plugin.getComponentLogger().warn("Webhooks are enabled but no url is configured");
            return;
        }
        if (client == null) {
            client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(5))
                    .build();
        }
    }

    public boolean enabled() {
        return enabled;
    }

    public void send(@NotNull String event, @NotNull Koth koth, @Nullable String playerName) {
        if (!enabled) {
            return;
        }
        String template = templates.get(event.toLowerCase(java.util.Locale.ROOT));
        if (template == null || template.isBlank()) {
            return;
        }
        String content = template
                .replace("%koth%", koth.name())
                .replace("%mode%", koth.mode().name())
                .replace("%player%", playerName == null ? "" : playerName);
        post(buildPayload(content));
    }

    private @NotNull String buildPayload(@NotNull String description) {
        return "{\"username\":" + json(username)
                + ",\"embeds\":[{\"description\":" + json(description)
                + ",\"color\":" + color + "}]}";
    }

    private void post(@NotNull String body) {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(10))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        client.sendAsync(request, HttpResponse.BodyHandlers.discarding())
                .thenAccept(response -> {
                    int status = response.statusCode();
                    if (status < 200 || status >= 300) {
                        plugin.getComponentLogger().warn("Discord webhook returned HTTP {}", status);
                    }
                })
                .exceptionally(throwable -> {
                    plugin.getComponentLogger().warn("Discord webhook failed: {}", throwable.getMessage());
                    return null;
                });
    }

    private static @NotNull String json(@NotNull String raw) {
        StringBuilder out = new StringBuilder(raw.length() + 2);
        out.append('"');
        for (int index = 0; index < raw.length(); index++) {
            char character = raw.charAt(index);
            switch (character) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                case '\b' -> out.append("\\b");
                case '\f' -> out.append("\\f");
                default -> {
                    if (character < 0x20) {
                        out.append(String.format("\\u%04x", (int) character));
                    } else {
                        out.append(character);
                    }
                }
            }
        }
        return out.append('"').toString();
    }
}
