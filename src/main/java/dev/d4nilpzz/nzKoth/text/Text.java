package dev.d4nilpzz.nzKoth.text;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Text {

    private static final MiniMessage MINI = MiniMessage.miniMessage();
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();
    private static final Pattern PLACEHOLDER = Pattern.compile("%([^%<>'\s]+)%");

    private static @Nullable PlaceholderResolver resolver;

    private Text() {
    }

    public static void placeholderResolver(@Nullable PlaceholderResolver value) {
        resolver = value;
    }

    public static boolean hasPlaceholders() {
        return resolver != null;
    }

    public static @NotNull Component parse(@NotNull String raw, @NotNull TagResolver... extra) {
        return parse(null, raw, extra);
    }

    public static @NotNull Component parse(@Nullable OfflinePlayer viewer,
                                           @NotNull String raw,
                                           @NotNull TagResolver... extra) {
        if (raw.isEmpty()) {
            return Component.empty();
        }
        List<TagResolver> resolvers = new ArrayList<>(extra.length + 1);
        resolvers.add(papiTag(viewer));
        for (TagResolver tag : extra) {
            resolvers.add(tag);
        }
        return MINI.deserialize(tagify(raw), TagResolver.resolver(resolvers));
    }

    public static @NotNull List<Component> parse(@Nullable OfflinePlayer viewer,
                                                 @NotNull List<String> lines,
                                                 @NotNull TagResolver... extra) {
        List<Component> out = new ArrayList<>(lines.size());
        for (String line : lines) {
            out.add(parse(viewer, line, extra));
        }
        return out;
    }

    public static @NotNull String escape(@NotNull String raw) {
        return MINI.escapeTags(raw);
    }

    private static @NotNull String tagify(@NotNull String raw) {
        Matcher matcher = PLACEHOLDER.matcher(raw);
        if (!matcher.find()) {
            return raw;
        }
        StringBuilder out = new StringBuilder(raw.length() + 16);
        matcher.reset();
        while (matcher.find()) {
            matcher.appendReplacement(out, Matcher.quoteReplacement("<papi:'" + matcher.group(1) + "'>"));
        }
        matcher.appendTail(out);
        return out.toString();
    }

    private static @NotNull TagResolver papiTag(@Nullable OfflinePlayer viewer) {
        return TagResolver.resolver("papi", (args, ctx) -> {
            String name = args.popOr("<papi:...> requires a placeholder name").value();
            PlaceholderResolver current = resolver;
            if (current == null) {
                return Tag.selfClosingInserting(Component.text('%' + name + '%'));
            }
            return Tag.selfClosingInserting(LEGACY.deserialize(current.resolve(viewer, name)));
        });
    }
}
