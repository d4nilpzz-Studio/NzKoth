package dev.d4nilpzz.nzKoth.display.menu;

import dev.d4nilpzz.nzKoth.NzKoth;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public final class ChatInput implements Listener {

    private record Request(@NotNull Consumer<String> onInput, @NotNull Runnable onCancel) {
    }

    private final NzKoth plugin;
    private final Map<UUID, Request> waiting = new ConcurrentHashMap<>();

    public ChatInput(@NotNull NzKoth plugin) {
        this.plugin = plugin;
    }

    public void ask(@NotNull Player player, @NotNull Component prompt,
                    @NotNull Consumer<String> onInput, @NotNull Runnable onCancel) {
        waiting.put(player.getUniqueId(), new Request(onInput, onCancel));
        player.closeInventory();
        player.sendMessage(plugin.messages().get(player, "chat-input.separator"));
        player.sendMessage(prompt);
        player.sendMessage(plugin.messages().get(player, "chat-input.cancel"));
        player.sendMessage(plugin.messages().get(player, "chat-input.separator"));
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(@NotNull AsyncChatEvent event) {
        Request request = waiting.remove(event.getPlayer().getUniqueId());
        if (request == null) {
            return;
        }
        event.setCancelled(true);
        String text = PlainTextComponentSerializer.plainText().serialize(event.message()).trim();
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (!event.getPlayer().isOnline()) {
                return;
            }
            if (text.isEmpty() || text.equalsIgnoreCase("cancel") || text.equalsIgnoreCase("cancelar")) {
                request.onCancel().run();
            } else {
                request.onInput().accept(text);
            }
        });
    }

    @EventHandler
    public void onQuit(@NotNull PlayerQuitEvent event) {
        waiting.remove(event.getPlayer().getUniqueId());
    }
}
