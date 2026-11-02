package dev.d4nilpzz.nzKoth.display;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

public final class Menus {

    private Menus() {
    }

    public static @NotNull ItemStack filler() {
        ItemStack stack = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta meta = stack.getItemMeta();
        meta.displayName(Component.empty());
        meta.setHideTooltip(true);
        stack.setItemMeta(meta);
        return stack;
    }

    public static void fill(@NotNull Inventory inventory, int from, int to) {
        ItemStack filler = filler();
        for (int slot = from; slot < to && slot < inventory.getSize(); slot++) {
            if (inventory.getItem(slot) == null) {
                inventory.setItem(slot, filler);
            }
        }
    }

    public static void fillEmpty(@NotNull Inventory inventory) {
        fill(inventory, 0, inventory.getSize());
    }
}
