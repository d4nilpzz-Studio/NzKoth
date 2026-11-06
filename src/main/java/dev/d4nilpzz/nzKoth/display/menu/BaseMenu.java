package dev.d4nilpzz.nzKoth.display.menu;

import dev.d4nilpzz.nzKoth.NzKoth;
import dev.d4nilpzz.nzKoth.display.Menus;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class BaseMenu implements InventoryHolder {

    @FunctionalInterface
    public interface Action {
        void run(@NotNull Player player, @NotNull ClickType click, @Nullable ItemStack cursor);
    }

    protected static final int SIZE = 54;

    protected final NzKoth plugin;
    private final Inventory inventory;
    private final Map<Integer, Action> actions = new HashMap<>();

    protected BaseMenu(@NotNull NzKoth plugin, @NotNull Component title) {
        this.plugin = plugin;
        this.inventory = plugin.getServer().createInventory(this, SIZE, title);
    }

    protected abstract void render();

    public void open(@NotNull Player player) {
        refresh();
        player.openInventory(inventory);
    }

    public void refresh() {
        inventory.clear();
        actions.clear();
        render();
        ItemStack filler = Menus.filler();
        for (int slot = 0; slot < SIZE; slot++) {
            ItemStack item = inventory.getItem(slot);
            if ((item == null || item.getType().isAir()) && !keepEmpty(slot)) {
                inventory.setItem(slot, filler);
            }
        }
    }

    protected boolean keepEmpty(int slot) {
        return false;
    }

    protected void set(int slot, @NotNull ItemStack item, @Nullable Action action) {
        inventory.setItem(slot, item);
        if (action != null) {
            actions.put(slot, action);
        }
    }

    protected void set(int slot, @NotNull ItemStack item) {
        set(slot, item, null);
    }

    protected @NotNull ItemStack icon(@NotNull Material material, @NotNull String key, @NotNull TagResolver... resolvers) {
        return icon(material, 1, key, resolvers);
    }

    protected @NotNull ItemStack icon(@NotNull Material material, int amount, @NotNull String key,
                                      @NotNull TagResolver... resolvers) {
        ItemStack stack = new ItemStack(material);
        stack.setAmount(Math.max(1, Math.min(99, amount)));
        ItemMeta meta = stack.getItemMeta();
        meta.displayName(plugin.messages().get(key + ".name", resolvers));
        meta.lore(plugin.messages().list(null, key + ".lore", resolvers));
        meta.addItemFlags(ItemFlag.values());
        stack.setItemMeta(meta);
        return stack;
    }

    protected @NotNull ItemStack withLore(@NotNull ItemStack base, @NotNull List<Component> extra) {
        ItemStack stack = base.clone();
        ItemMeta meta = stack.getItemMeta();
        List<Component> lore = new ArrayList<>();
        List<Component> original = meta.lore();
        if (original != null) {
            lore.addAll(original);
        }
        lore.addAll(extra);
        meta.lore(lore);
        stack.setItemMeta(meta);
        return stack;
    }

    protected void backButton(int slot, @NotNull Action action) {
        set(slot, icon(Material.ARROW, "menu.back"), (player, click, cursor) -> {
            click(player);
            action.run(player, click, cursor);
        });
    }

    public void handleClick(@NotNull Player player, int slot, @NotNull ClickType click, @Nullable ItemStack cursor) {
        Action action = actions.get(slot);
        if (action != null) {
            action.run(player, click, cursor);
        } else {
            handleEmptyClick(player, slot, click, cursor);
        }
    }

    protected void handleEmptyClick(@NotNull Player player, int slot, @NotNull ClickType click,
                                    @Nullable ItemStack cursor) {
    }

    public void handleInventoryClick(@NotNull Player player, @NotNull ItemStack clicked, @NotNull ClickType click) {
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }

    public static void click(@NotNull Player player) {
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.2f);
    }

    public static void deny(@NotNull Player player) {
        player.playSound(player.getLocation(), Sound.BLOCK_VAULT_INSERT_ITEM_FAIL, 0.6f, 1f);
    }

    public static void success(@NotNull Player player) {
        player.playSound(player.getLocation(), Sound.BLOCK_VAULT_INSERT_ITEM, 0.6f, 1.2f);
    }
}
