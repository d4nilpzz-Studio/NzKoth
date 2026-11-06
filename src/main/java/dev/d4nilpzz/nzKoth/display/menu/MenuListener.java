package dev.d4nilpzz.nzKoth.display.menu;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public final class MenuListener implements Listener {

    @EventHandler
    public void onClick(@NotNull InventoryClickEvent event) {
        if (!(event.getInventory().getHolder(false) instanceof BaseMenu menu)) {
            return;
        }
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player) || event.getClickedInventory() == null) {
            return;
        }
        if (event.getClickedInventory() == event.getView().getTopInventory()) {
            ItemStack cursor = event.getCursor();
            menu.handleClick(player, event.getSlot(), event.getClick(),
                    cursor.isEmpty() ? null : cursor.clone());
        } else {
            ItemStack clicked = event.getCurrentItem();
            if (clicked != null && !clicked.isEmpty()) {
                menu.handleInventoryClick(player, clicked.clone(), event.getClick());
            }
        }
    }

    @EventHandler
    public void onDrag(@NotNull InventoryDragEvent event) {
        if (event.getInventory().getHolder(false) instanceof BaseMenu) {
            event.setCancelled(true);
        }
    }
}
