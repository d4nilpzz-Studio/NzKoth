package dev.d4nilpzz.nzKoth.display;

import dev.d4nilpzz.nzKoth.NzKoth;
import dev.d4nilpzz.nzKoth.koth.Koth;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class RewardsEditor implements Listener {

    private static final int SIZE = 54;
    private static final int MAX_ENTRIES = 45;
    private static final int BOTTOM_ROW = 45;
    private static final int SLOT_BACK = 45;
    private static final int SLOT_ADD_COMMAND = 48;
    private static final int SLOT_HELP = 49;
    private static final int SLOT_ADD_BROADCAST = 50;

    private enum Kind {
        COMMAND,
        BROADCAST
    }

    private record Entry(@NotNull Kind kind, int index, @NotNull String command) {
    }

    private record Pending(@NotNull Koth koth, @NotNull Kind kind, int index) {
    }

    private static final class RewardsMenu implements InventoryHolder {
        private final Koth koth;
        private Inventory inventory;

        private RewardsMenu(@NotNull Koth koth) {
            this.koth = koth;
        }

        @Override
        public @NotNull Inventory getInventory() {
            return inventory;
        }
    }

    private final NzKoth plugin;
    private final Map<UUID, Pending> pending = new ConcurrentHashMap<>();

    public RewardsEditor(@NotNull NzKoth plugin) {
        this.plugin = plugin;
    }

    public void open(@NotNull Player player, @NotNull Koth koth) {
        RewardsMenu menu = new RewardsMenu(koth);
        menu.inventory = plugin.getServer().createInventory(menu, SIZE,
                plugin.messages().get("rewards.title", Placeholder.unparsed("koth", koth.name())));
        render(menu);
        player.openInventory(menu.inventory);
    }

    private static @NotNull List<Entry> entries(@NotNull Koth koth) {
        List<Entry> out = new ArrayList<>();
        List<String> commands = koth.rewards().commands();
        for (int index = 0; index < commands.size(); index++) {
            out.add(new Entry(Kind.COMMAND, index, commands.get(index)));
        }
        List<String> broadcast = koth.rewards().broadcastCommands();
        for (int index = 0; index < broadcast.size(); index++) {
            out.add(new Entry(Kind.BROADCAST, index, broadcast.get(index)));
        }
        return out;
    }

    private static @NotNull List<String> list(@NotNull Koth koth, @NotNull Kind kind) {
        return kind == Kind.COMMAND ? koth.rewards().commands() : koth.rewards().broadcastCommands();
    }

    private void render(@NotNull RewardsMenu menu) {
        Inventory inventory = menu.inventory;
        inventory.clear();
        List<Entry> entries = entries(menu.koth);
        for (int slot = 0; slot < MAX_ENTRIES && slot < entries.size(); slot++) {
            Entry entry = entries.get(slot);
            boolean command = entry.kind() == Kind.COMMAND;
            inventory.setItem(slot, item(command ? Material.PAPER : Material.BELL,
                    command ? "rewards.command" : "rewards.broadcast-command",
                    Placeholder.unparsed("number", String.valueOf(entry.index() + 1)),
                    Placeholder.unparsed("command", entry.command())));
        }
        inventory.setItem(SLOT_BACK, item(Material.ARROW, "rewards.back"));
        inventory.setItem(SLOT_ADD_COMMAND, item(Material.WRITABLE_BOOK, "rewards.add-command"));
        inventory.setItem(SLOT_HELP, item(Material.BOOK, "rewards.help"));
        inventory.setItem(SLOT_ADD_BROADCAST, item(Material.WRITABLE_BOOK, "rewards.add-broadcast"));
        Menus.fill(inventory, BOTTOM_ROW, SIZE);
    }

    private @NotNull ItemStack item(@NotNull Material material, @NotNull String key, TagResolver... resolvers) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        meta.displayName(plugin.messages().get(key + ".name", resolvers));
        meta.lore(plugin.messages().list(null, key + ".lore", resolvers));
        stack.setItemMeta(meta);
        return stack;
    }

    @EventHandler
    public void onDrag(@NotNull InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof RewardsMenu) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onClick(@NotNull InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        Inventory top = event.getView().getTopInventory();
        if (!(top.getHolder() instanceof RewardsMenu menu)) {
            return;
        }
        event.setCancelled(true);
        if (event.getClickedInventory() == null || !event.getClickedInventory().equals(top)) {
            return;
        }
        Koth koth = menu.koth;
        int slot = event.getSlot();
        switch (slot) {
            case SLOT_BACK -> {
                plugin.editor().open(player, koth);
                return;
            }
            case SLOT_ADD_COMMAND -> {
                prompt(player, new Pending(koth, Kind.COMMAND, -1), null);
                return;
            }
            case SLOT_ADD_BROADCAST -> {
                prompt(player, new Pending(koth, Kind.BROADCAST, -1), null);
                return;
            }
            default -> {
            }
        }

        List<Entry> entries = entries(koth);
        if (slot >= MAX_ENTRIES || slot >= entries.size()) {
            return;
        }
        Entry entry = entries.get(slot);
        List<String> source = list(koth, entry.kind());
        ClickType click = event.getClick();
        switch (click) {
            case LEFT -> {
                prompt(player, new Pending(koth, entry.kind(), entry.index()), entry.command());
                return;
            }
            case RIGHT -> {
                source.remove(entry.index());
                list(koth, entry.kind() == Kind.COMMAND ? Kind.BROADCAST : Kind.COMMAND).add(entry.command());
            }
            case SHIFT_LEFT -> {
                if (entry.index() == 0) {
                    return;
                }
                source.set(entry.index(), source.get(entry.index() - 1));
                source.set(entry.index() - 1, entry.command());
            }
            case SHIFT_RIGHT -> source.remove(entry.index());
            default -> {
                return;
            }
        }
        plugin.koths().save(koth);
        render(menu);
    }

    private void prompt(@NotNull Player player, @NotNull Pending request, @Nullable String current) {
        if (request.index() < 0 && entries(request.koth()).size() >= MAX_ENTRIES) {
            player.sendMessage(plugin.messages().get(player, "rewards.full"));
            return;
        }
        pending.put(player.getUniqueId(), request);
        player.closeInventory();
        player.sendMessage(plugin.messages().get(player,
                request.index() < 0 ? "rewards.prompt-add" : "rewards.prompt-edit"));
        if (current != null) {
            player.sendMessage(plugin.messages().get(player, "rewards.current",
                            Placeholder.unparsed("command", current))
                    .clickEvent(ClickEvent.suggestCommand(current)));
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(@NotNull AsyncChatEvent event) {
        Player player = event.getPlayer();
        Pending request = pending.remove(player.getUniqueId());
        if (request == null) {
            return;
        }
        event.setCancelled(true);
        String input = PlainTextComponentSerializer.plainText().serialize(event.message()).trim();
        plugin.getServer().getScheduler().runTask(plugin, () -> apply(player, request, input));
    }

    private void apply(@NotNull Player player, @NotNull Pending request, @NotNull String input) {
        if (!player.isOnline()) {
            return;
        }
        String command = input.startsWith("/") ? input.substring(1).trim() : input;
        if (input.equalsIgnoreCase("cancel") || command.isEmpty()) {
            player.sendMessage(plugin.messages().get(player, "rewards.cancelled"));
            open(player, request.koth());
            return;
        }
        List<String> target = list(request.koth(), request.kind());
        if (request.index() >= 0 && request.index() < target.size()) {
            target.set(request.index(), command);
        } else {
            target.add(command);
        }
        plugin.koths().save(request.koth());
        player.sendMessage(plugin.messages().get(player, "rewards.saved",
                Placeholder.unparsed("command", command)));
        open(player, request.koth());
    }

    @EventHandler
    public void onQuit(@NotNull PlayerQuitEvent event) {
        pending.remove(event.getPlayer().getUniqueId());
    }
}
