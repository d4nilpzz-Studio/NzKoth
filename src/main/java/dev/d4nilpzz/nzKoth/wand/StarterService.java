package dev.d4nilpzz.nzKoth.wand;

import dev.d4nilpzz.nzKoth.NzKoth;
import dev.d4nilpzz.nzKoth.koth.Koth;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class StarterService implements Listener {

    private final NzKoth plugin;
    private final NamespacedKey key;

    public StarterService(@NotNull NzKoth plugin) {
        this.plugin = plugin;
        this.key = new NamespacedKey(plugin, "starter");
    }

    public @NotNull ItemStack create(@NotNull Koth koth, int amount) {
        Material material = Material.matchMaterial(
                plugin.getConfig().getString("starter.material", "NETHER_STAR"));
        ItemStack item = new ItemStack(material == null ? Material.NETHER_STAR : material, amount);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(plugin.messages().get("starter.name",
                Placeholder.unparsed("koth", koth.name())));
        meta.lore(plugin.messages().list(null, "starter.lore",
                Placeholder.unparsed("koth", koth.name()),
                Placeholder.unparsed("mode", koth.mode().name())));
        int modelData = plugin.getConfig().getInt("starter.custom-model-data", 0);
        if (modelData > 0) {
            meta.setCustomModelData(modelData);
        }
        meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, koth.lowerName());
        item.setItemMeta(meta);
        return item;
    }

    public @Nullable String kothOf(@Nullable ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return null;
        }
        return item.getItemMeta().getPersistentDataContainer().get(key, PersistentDataType.STRING);
    }

    @EventHandler(ignoreCancelled = true)
    public void onInteract(@NotNull PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        ItemStack item = event.getItem();
        String kothName = kothOf(item);
        if (kothName == null) {
            return;
        }
        event.setCancelled(true);
        Player player = event.getPlayer();
        Koth koth = plugin.koths().get(kothName);
        if (koth == null) {
            player.sendMessage(plugin.messages().get(player, "starter.unknown-koth",
                    Placeholder.unparsed("koth", kothName)));
            return;
        }
        if (plugin.sessions().isRunning(koth)) {
            player.sendMessage(plugin.messages().get(player, "command.already-running",
                    Placeholder.unparsed("koth", koth.name())));
            return;
        }
        if (!plugin.sessions().start(koth)) {
            player.sendMessage(plugin.messages().get(player, "command.not-configured",
                    Placeholder.unparsed("koth", koth.name())));
            return;
        }
        item.setAmount(item.getAmount() - 1);
        player.sendMessage(plugin.messages().get(player, "starter.used",
                Placeholder.unparsed("koth", koth.name())));
    }
}
