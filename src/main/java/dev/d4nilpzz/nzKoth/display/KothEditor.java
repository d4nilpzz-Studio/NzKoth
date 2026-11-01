package dev.d4nilpzz.nzKoth.display;

import dev.d4nilpzz.nzKoth.NzKoth;
import dev.d4nilpzz.nzKoth.koth.Koth;
import dev.d4nilpzz.nzKoth.koth.KothMode;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class KothEditor implements Listener {

    private static final int SLOT_MODE = 10;
    private static final int SLOT_CAPTURE_TIME = 12;
    private static final int SLOT_TARGET_SCORE = 14;
    private static final int SLOT_MAX_TIME = 16;
    private static final int SLOT_BOSSBAR = 28;
    private static final int SLOT_BROADCAST = 30;
    private static final int SLOT_CROWN = 32;
    private static final int SLOT_CROWN_MODEL = 34;

    private final NzKoth plugin;
    private final Map<UUID, Koth> editing = new HashMap<>();

    public KothEditor(@NotNull NzKoth plugin) {
        this.plugin = plugin;
    }

    public void open(@NotNull Player player, @NotNull Koth koth) {
        Inventory inventory = plugin.getServer().createInventory(null, 45,
                plugin.messages().get("editor.title", Placeholder.unparsed("koth", koth.name())));
        editing.put(player.getUniqueId(), koth);
        render(inventory, koth);
        player.openInventory(inventory);
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
        inventory.setItem(SLOT_CROWN, toggle(koth.crownEnabled(), "editor.crown"));
        inventory.setItem(SLOT_CROWN_MODEL, item(Material.GOLDEN_HELMET, "editor.crown-model",
                Placeholder.unparsed("value", String.valueOf(koth.crownCustomModelData()))));
    }

    private @NotNull ItemStack item(@NotNull Material material,
                                    @NotNull String key,
                                    net.kyori.adventure.text.minimessage.tag.resolver.TagResolver... resolvers) {
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
    public void onClick(@NotNull InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        Koth koth = editing.get(player.getUniqueId());
        if (koth == null || !event.getInventory().equals(event.getView().getTopInventory())) {
            return;
        }
        if (event.getClickedInventory() == null || !event.getClickedInventory().equals(event.getView().getTopInventory())) {
            return;
        }
        event.setCancelled(true);

        int delta = switch (event.getClick()) {
            case LEFT -> 1;
            case RIGHT -> -1;
            case SHIFT_LEFT -> 10;
            case SHIFT_RIGHT -> -10;
            default -> 0;
        };

        switch (event.getSlot()) {
            case SLOT_MODE -> koth.mode(koth.mode() == KothMode.CAPTURE ? KothMode.SCORE : KothMode.CAPTURE);
            case SLOT_CAPTURE_TIME -> koth.captureTime(
                    Duration.ofSeconds(Math.max(1, koth.captureTime().toSeconds() + delta)));
            case SLOT_TARGET_SCORE -> koth.targetScore(Math.max(1, koth.targetScore() + delta));
            case SLOT_MAX_TIME -> koth.maxTime(
                    Duration.ofSeconds(Math.max(1, koth.maxTime().toSeconds() + delta)));
            case SLOT_BOSSBAR -> koth.bossBarEnabled(!koth.bossBarEnabled());
            case SLOT_BROADCAST -> koth.broadcastEnabled(!koth.broadcastEnabled());
            case SLOT_CROWN -> koth.crownEnabled(!koth.crownEnabled());
            case SLOT_CROWN_MODEL -> koth.crownCustomModelData(
                    Math.max(0, koth.crownCustomModelData() + delta));
            default -> {
                return;
            }
        }
        plugin.koths().save(koth);
        render(event.getInventory(), koth);
    }

    @EventHandler
    public void onClose(@NotNull InventoryCloseEvent event) {
        editing.remove(event.getPlayer().getUniqueId());
    }

    public boolean isEditing(@NotNull Player player) {
        return editing.containsKey(player.getUniqueId());
    }

    public @NotNull ClickType noop() {
        return ClickType.UNKNOWN;
    }
}
