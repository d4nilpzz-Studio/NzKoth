package dev.d4nilpzz.nzKoth.wand;

import dev.d4nilpzz.nzKoth.NzKoth;
import dev.d4nilpzz.nzKoth.text.Text;
import dev.d4nilpzz.nzKoth.util.Cuboid;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class WandService implements Listener {

    private final NzKoth plugin;
    private final NamespacedKey key;
    private final Map<UUID, Location> first = new HashMap<>();
    private final Map<UUID, Location> second = new HashMap<>();

    public WandService(@NotNull NzKoth plugin) {
        this.plugin = plugin;
        this.key = new NamespacedKey(plugin, "wand");
    }

    public @NotNull ItemStack createWand() {
        Material material = Material.matchMaterial(plugin.settings().wandMaterial());
        ItemStack item = new ItemStack(material == null ? Material.GOLDEN_AXE : material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(plugin.messages().get("wand.name"));
        meta.lore(plugin.messages().list(null, "wand.lore"));
        if (plugin.settings().wandCustomModelData() > 0) {
            meta.setCustomModelData(plugin.settings().wandCustomModelData());
        }
        meta.getPersistentDataContainer().set(key, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    public boolean isWand(@Nullable ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }
        return item.getItemMeta().getPersistentDataContainer().has(key, PersistentDataType.BYTE);
    }

    public @Nullable Cuboid selectionOf(@NotNull Player player) {
        Location a = first.get(player.getUniqueId());
        Location b = second.get(player.getUniqueId());
        if (a == null || b == null) {
            return null;
        }
        return Cuboid.between(a, b);
    }

    public void clear(@NotNull UUID playerId) {
        first.remove(playerId);
        second.remove(playerId);
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onInteract(@NotNull PlayerInteractEvent event) {
        if (!isWand(event.getItem())) {
            return;
        }
        Block block = event.getClickedBlock();
        if (block == null) {
            return;
        }
        Player player = event.getPlayer();
        if (!player.hasPermission("nzkoth.admin")) {
            return;
        }
        if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
            event.setCancelled(true);
            first.put(player.getUniqueId(), block.getLocation());
            notifySelection(player, "wand.first", block);
        } else if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            event.setCancelled(true);
            second.put(player.getUniqueId(), block.getLocation());
            notifySelection(player, "wand.second", block);
        }
    }

    @EventHandler
    public void onQuit(@NotNull PlayerQuitEvent event) {
        clear(event.getPlayer().getUniqueId());
    }

    private void notifySelection(@NotNull Player player, @NotNull String messageKey, @NotNull Block block) {
        player.sendMessage(plugin.messages().get(player, messageKey,
                Placeholder.unparsed("x", String.valueOf(block.getX())),
                Placeholder.unparsed("y", String.valueOf(block.getY())),
                Placeholder.unparsed("z", String.valueOf(block.getZ()))));
        Cuboid selection = selectionOf(player);
        if (selection != null) {
            player.sendMessage(plugin.messages().get(player, "wand.ready",
                    Placeholder.unparsed("blocks", String.valueOf(selection.volume()))));
        }
    }

    public @NotNull String noSelectionMessage() {
        return Text.escape(plugin.messages().raw("wand.none"));
    }
}
