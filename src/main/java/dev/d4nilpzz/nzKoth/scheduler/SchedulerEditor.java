package dev.d4nilpzz.nzKoth.scheduler;

import dev.d4nilpzz.nzKoth.NzKoth;
import dev.d4nilpzz.nzKoth.util.CronExpression;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class SchedulerEditor implements Listener {

    private static final int SIZE = 54;
    private static final int SLOT_ADD = 49;
    private static final DateTimeFormatter NEXT_RUN = DateTimeFormatter.ofPattern("dd/MM HH:mm");

    private final NzKoth plugin;
    private final Set<UUID> viewing = new HashSet<>();
    private final Set<UUID> awaitingInput = new HashSet<>();

    public SchedulerEditor(@NotNull NzKoth plugin) {
        this.plugin = plugin;
    }

    public void open(@NotNull Player player) {
        Inventory inventory = plugin.getServer().createInventory(null, SIZE,
                plugin.messages().get("scheduler.title",
                        Placeholder.unparsed("amount", String.valueOf(plugin.schedulers().size()))));
        render(inventory);
        viewing.add(player.getUniqueId());
        player.openInventory(inventory);
    }

    private void render(@NotNull Inventory inventory) {
        inventory.clear();
        List<Schedule> all = plugin.schedulers().all();
        LocalDateTime now = LocalDateTime.now();
        for (int index = 0; index < all.size() && index < SLOT_ADD; index++) {
            inventory.setItem(index, itemFor(all.get(index), now));
        }
        inventory.setItem(SLOT_ADD, addButton());
    }

    private @NotNull ItemStack itemFor(@NotNull Schedule schedule, @NotNull LocalDateTime now) {
        ItemStack stack = new ItemStack(schedule.enabled() ? Material.CLOCK : Material.GRAY_DYE);
        ItemMeta meta = stack.getItemMeta();
        LocalDateTime next = schedule.nextRun(now);
        meta.displayName(plugin.messages().get("scheduler.entry.name",
                Placeholder.unparsed("id", schedule.id()),
                Placeholder.unparsed("koth", schedule.kothName())));
        meta.lore(plugin.messages().list(null, "scheduler.entry.lore",
                Placeholder.unparsed("cron", schedule.cron().raw()),
                Placeholder.unparsed("action", schedule.action().name()),
                Placeholder.unparsed("state", schedule.enabled() ? "enabled" : "disabled"),
                Placeholder.unparsed("next", next == null ? "never" : NEXT_RUN.format(next))));
        stack.setItemMeta(meta);
        return stack;
    }

    private @NotNull ItemStack addButton() {
        ItemStack stack = new ItemStack(Material.EMERALD);
        ItemMeta meta = stack.getItemMeta();
        meta.displayName(plugin.messages().get("scheduler.add.name"));
        meta.lore(plugin.messages().list(null, "scheduler.add.lore"));
        stack.setItemMeta(meta);
        return stack;
    }

    @EventHandler
    public void onClick(@NotNull InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player) || !viewing.contains(player.getUniqueId())) {
            return;
        }
        if (event.getClickedInventory() == null
                || !event.getClickedInventory().equals(event.getView().getTopInventory())) {
            return;
        }
        event.setCancelled(true);

        if (event.getSlot() == SLOT_ADD) {
            awaitingInput.add(player.getUniqueId());
            player.closeInventory();
            player.sendMessage(plugin.messages().get(player, "scheduler.prompt"));
            return;
        }

        List<Schedule> all = plugin.schedulers().all();
        if (event.getSlot() >= all.size()) {
            return;
        }
        Schedule schedule = all.get(event.getSlot());
        switch (event.getClick()) {
            case LEFT -> schedule.enabled(!schedule.enabled());
            case RIGHT -> schedule.action(schedule.action() == ScheduleAction.START
                    ? ScheduleAction.STOP
                    : ScheduleAction.START);
            case SHIFT_RIGHT -> plugin.schedulers().remove(schedule.id());
            default -> {
                return;
            }
        }
        plugin.schedulers().save();
        render(event.getInventory());
    }

    @EventHandler
    public void onClose(@NotNull InventoryCloseEvent event) {
        viewing.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onQuit(@NotNull PlayerQuitEvent event) {
        viewing.remove(event.getPlayer().getUniqueId());
        awaitingInput.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onChat(@NotNull AsyncChatEvent event) {
        Player player = event.getPlayer();
        if (!awaitingInput.contains(player.getUniqueId())) {
            return;
        }
        event.setCancelled(true);
        awaitingInput.remove(player.getUniqueId());
        String input = PlainTextComponentSerializer.plainText().serialize(event.message()).trim();

        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (input.equalsIgnoreCase("cancel")) {
                player.sendMessage(plugin.messages().get(player, "scheduler.cancelled"));
                return;
            }
            parseInput(player, input);
        });
    }

    private void parseInput(@NotNull Player player, @NotNull String input) {
        String[] parts = input.split("\\s+");
        if (parts.length < 7) {
            player.sendMessage(plugin.messages().get(player, "scheduler.bad-input"));
            return;
        }
        String kothName = parts[0];
        if (plugin.koths().get(kothName) == null) {
            player.sendMessage(plugin.messages().get(player, "command.not-configured",
                    Placeholder.unparsed("koth", kothName)));
            return;
        }
        ScheduleAction action = ScheduleAction.parse(parts[1]);
        if (action == null) {
            player.sendMessage(plugin.messages().get(player, "scheduler.bad-action",
                    Placeholder.unparsed("action", parts[1])));
            return;
        }
        String expression = String.join(" ", java.util.Arrays.copyOfRange(parts, 2, parts.length));
        if (!CronExpression.isValid(expression)) {
            player.sendMessage(plugin.messages().get(player, "scheduler.bad-cron",
                    Placeholder.unparsed("cron", expression)));
            return;
        }
        Schedule created = plugin.schedulers().add(kothName, CronExpression.parse(expression), action);
        LocalDateTime next = created.nextRun(LocalDateTime.now());
        player.sendMessage(plugin.messages().get(player, "scheduler.created",
                Placeholder.unparsed("id", created.id()),
                Placeholder.unparsed("koth", kothName),
                Placeholder.unparsed("cron", expression),
                Placeholder.unparsed("next", next == null ? "never" : NEXT_RUN.format(next))));
        open(player);
    }
}
