package dev.d4nilpzz.nzKoth.display;

import dev.d4nilpzz.nzKoth.NzKoth;
import dev.d4nilpzz.nzKoth.koth.Koth;
import dev.d4nilpzz.nzKoth.koth.KothMode;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.List;
import java.util.Locale;

public final class KothEditor implements Listener {

    private static final int SIZE = 54;

    private static final int SLOT_MODE = 10;
    private static final int SLOT_CAPTURE_TIME = 12;
    private static final int SLOT_TARGET_SCORE = 14;
    private static final int SLOT_MAX_TIME = 16;
    private static final int SLOT_BOSSBAR = 28;
    private static final int SLOT_BROADCAST = 29;
    private static final int SLOT_CROWN = 30;
    private static final int SLOT_HOLOGRAM = 31;
    private static final int SLOT_VIEW_DISTANCE = 32;
    private static final int SLOT_CROWN_OUTLINE = 33;
    private static final int SLOT_CROWN_MODEL = 34;
    private static final int SLOT_CROWN_GLOW = 37;
    private static final int SLOT_CROWN_GLINT = 38;
    private static final int SLOT_REWARDS = 42;
    private static final int SLOT_CROWN_HEIGHT = 43;

    private static final List<String> OUTLINE_COLORS = List.of(
            "gold", "yellow", "red", "aqua", "green", "light_purple", "white", "blue");

    private final NzKoth plugin;

    public KothEditor(@NotNull NzKoth plugin) {
        this.plugin = plugin;
    }

    private static final class EditorMenu implements InventoryHolder {
        private final Koth koth;
        private Inventory inventory;

        private EditorMenu(@NotNull Koth koth) {
            this.koth = koth;
        }

        @Override
        public @NotNull Inventory getInventory() {
            return inventory;
        }
    }

    public void open(@NotNull Player player, @NotNull Koth koth) {
        EditorMenu menu = new EditorMenu(koth);
        menu.inventory = plugin.getServer().createInventory(menu, SIZE,
                plugin.messages().get("editor.title", Placeholder.unparsed("koth", koth.name())));
        render(menu.inventory, koth);
        player.openInventory(menu.inventory);
    }

    private void render(@NotNull Inventory inventory, @NotNull Koth koth) {
        inventory.clear();
        inventory.setItem(SLOT_MODE, item(Material.COMPARATOR, "editor.mode",
                Placeholder.unparsed("value", koth.mode().name())));
        inventory.setItem(SLOT_CAPTURE_TIME, item(Material.CLOCK, "editor.capture-time",
                Placeholder.unparsed("value", String.valueOf(koth.captureTime().toSeconds()))));
        inventory.setItem(SLOT_TARGET_SCORE, item(Material.EXPERIENCE_BOTTLE, "editor.target-score",
                Placeholder.unparsed("value", String.valueOf(koth.targetScore()))));
        inventory.setItem(SLOT_MAX_TIME, item(Material.REPEATER, "editor.max-time",
                Placeholder.unparsed("value", String.valueOf(koth.maxTime().toSeconds()))));
        inventory.setItem(SLOT_BOSSBAR, toggle(koth.bossBarEnabled(), "editor.bossbar"));
        inventory.setItem(SLOT_BROADCAST, toggle(koth.broadcastEnabled(), "editor.broadcast"));
        inventory.setItem(SLOT_HOLOGRAM, toggle(koth.hologramEnabled(), "editor.hologram"));
        inventory.setItem(SLOT_VIEW_DISTANCE, item(Material.ENDER_EYE, "editor.view-distance",
                Placeholder.unparsed("value", String.valueOf(koth.viewDistance()))));
        inventory.setItem(SLOT_CROWN, toggle(koth.crownEnabled(), "editor.crown"));
        inventory.setItem(SLOT_CROWN_MODEL, item(Material.GOLDEN_HELMET, "editor.crown-model",
                Placeholder.unparsed("value", String.valueOf(koth.crownCustomModelData()))));
        inventory.setItem(SLOT_CROWN_GLOW, toggle(koth.crownGlow(), "editor.crown-glow"));
        inventory.setItem(SLOT_CROWN_GLINT, toggle(koth.crownGlint(), "editor.crown-glint"));
        inventory.setItem(SLOT_CROWN_OUTLINE, item(
                koth.crownOutline() ? Material.GLOWSTONE_DUST : Material.GUNPOWDER, "editor.crown-outline",
                Placeholder.unparsed("value", koth.crownOutline() ? "enabled" : "disabled"),
                Placeholder.unparsed("color", koth.crownOutlineColor())));
        inventory.setItem(SLOT_CROWN_HEIGHT, item(Material.SCAFFOLDING, "editor.crown-height",
                Placeholder.unparsed("value", String.format(Locale.ROOT, "%.1f", koth.crownHeight()))));
        inventory.setItem(SLOT_REWARDS, item(Material.CHEST, "editor.rewards",
                Placeholder.unparsed("commands", String.valueOf(koth.rewards().commands().size())),
                Placeholder.unparsed("broadcast", String.valueOf(koth.rewards().broadcastCommands().size())),
                Placeholder.unparsed("items", String.valueOf(koth.rewards().items().size()))));
        Menus.fillEmpty(inventory);
    }

    private @NotNull ItemStack item(@NotNull Material material, @NotNull String key, TagResolver... resolvers) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        meta.displayName(plugin.messages().get(key + ".name", resolvers));
        meta.lore(plugin.messages().list(null, key + ".lore", resolvers));
        stack.setItemMeta(meta);
        return stack;
    }

    private @NotNull ItemStack toggle(boolean enabled, @NotNull String key) {
        return item(enabled ? Material.LIME_DYE : Material.GRAY_DYE, key,
                Placeholder.unparsed("value", enabled ? "enabled" : "disabled"));
    }

    @EventHandler
    public void onDrag(@NotNull InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof EditorMenu) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onClick(@NotNull InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        Inventory top = event.getView().getTopInventory();
        if (!(top.getHolder() instanceof EditorMenu menu)) {
            return;
        }
        event.setCancelled(true);
        if (event.getClickedInventory() == null || !event.getClickedInventory().equals(top)) {
            return;
        }
        Koth koth = menu.koth;

        int delta = switch (event.getClick()) {
            case LEFT -> 1;
            case RIGHT -> -1;
            case SHIFT_LEFT -> 10;
            case SHIFT_RIGHT -> -10;
            default -> 0;
        };

        switch (event.getSlot()) {
            case SLOT_REWARDS -> {
                plugin.rewardsEditor().open(player, koth);
                return;
            }
            case SLOT_MODE -> koth.mode(koth.mode() == KothMode.CAPTURE ? KothMode.SCORE : KothMode.CAPTURE);
            case SLOT_CAPTURE_TIME -> koth.captureTime(
                    Duration.ofSeconds(Math.max(1, koth.captureTime().toSeconds() + delta)));
            case SLOT_TARGET_SCORE -> koth.targetScore(Math.max(1, koth.targetScore() + delta));
            case SLOT_MAX_TIME -> koth.maxTime(
                    Duration.ofSeconds(Math.max(1, koth.maxTime().toSeconds() + delta)));
            case SLOT_BOSSBAR -> koth.bossBarEnabled(!koth.bossBarEnabled());
            case SLOT_BROADCAST -> koth.broadcastEnabled(!koth.broadcastEnabled());
            case SLOT_HOLOGRAM -> koth.hologramEnabled(!koth.hologramEnabled());
            case SLOT_VIEW_DISTANCE -> {
                if (delta == 0) {
                    return;
                }
                koth.viewDistance(Math.max(-1, koth.viewDistance() + delta));
            }
            case SLOT_CROWN -> koth.crownEnabled(!koth.crownEnabled());
            case SLOT_CROWN_MODEL -> koth.crownCustomModelData(
                    Math.max(0, koth.crownCustomModelData() + delta));
            case SLOT_CROWN_GLOW -> koth.crownGlow(!koth.crownGlow());
            case SLOT_CROWN_GLINT -> koth.crownGlint(!koth.crownGlint());
            case SLOT_CROWN_OUTLINE -> {
                if (event.getClick() == ClickType.RIGHT) {
                    koth.crownOutlineColor(nextColor(koth.crownOutlineColor()));
                } else {
                    koth.crownOutline(!koth.crownOutline());
                }
            }
            case SLOT_CROWN_HEIGHT -> koth.crownHeight(
                    Math.max(0.5, Math.round((koth.crownHeight() + delta * 0.1) * 10.0) / 10.0));
            default -> {
                return;
            }
        }
        plugin.koths().save(koth);
        render(top, koth);
    }

    private static @NotNull String nextColor(@NotNull String current) {
        int index = OUTLINE_COLORS.indexOf(current.toLowerCase(Locale.ROOT));
        return OUTLINE_COLORS.get((index + 1) % OUTLINE_COLORS.size());
    }
}
