package dev.d4nilpzz.nzKoth.scheduler;

import dev.d4nilpzz.nzKoth.NzKoth;
import dev.d4nilpzz.nzKoth.display.Menus;
import dev.d4nilpzz.nzKoth.util.CronExpression;
import net.kyori.adventure.text.Component;
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

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public final class SchedulerEditor implements Listener {

    private static final int SIZE = 54;
    private static final int PAGE_SIZE = 45;
    private static final int BOTTOM_ROW = 45;

    private static final int LIST_PREVIOUS = 45;
    private static final int LIST_ADD = 49;
    private static final int LIST_NEXT = 53;

    private static final int EDIT_PREVIEW = 4;
    private static final int EDIT_KOTH = 19;
    private static final int EDIT_ACTION = 21;
    private static final int EDIT_FREQUENCY = 23;
    private static final int EDIT_ENABLED = 25;
    private static final int EDIT_HOUR = 30;
    private static final int EDIT_MINUTE = 32;
    private static final int EDIT_ALL_DAYS = 36;
    private static final int EDIT_FIRST_DAY = 37;
    private static final int EDIT_BACK = 45;
    private static final int EDIT_SAVE = 49;
    private static final int EDIT_DELETE = 53;

    private static final int[] DAY_ORDER = {1, 2, 3, 4, 5, 6, 0};
    private static final DateTimeFormatter NEXT_RUN = DateTimeFormatter.ofPattern("dd/MM HH:mm");

    private final NzKoth plugin;

    public SchedulerEditor(@NotNull NzKoth plugin) {
        this.plugin = plugin;
    }

    private static final class ListMenu implements InventoryHolder {
        private final int page;
        private Inventory inventory;

        private ListMenu(int page) {
            this.page = page;
        }

        @Override
        public @NotNull Inventory getInventory() {
            return inventory;
        }
    }

    private static final class EditMenu implements InventoryHolder {
        private final ScheduleDraft draft;
        private Inventory inventory;

        private EditMenu(@NotNull ScheduleDraft draft) {
            this.draft = draft;
        }

        @Override
        public @NotNull Inventory getInventory() {
            return inventory;
        }
    }

    public void open(@NotNull Player player) {
        openList(player, 0);
    }

    private void openList(@NotNull Player player, int page) {
        int pages = Math.max(1, (plugin.schedulers().size() + PAGE_SIZE - 1) / PAGE_SIZE);
        ListMenu menu = new ListMenu(Math.max(0, Math.min(page, pages - 1)));
        menu.inventory = plugin.getServer().createInventory(menu, SIZE,
                plugin.messages().get("scheduler.title",
                        Placeholder.unparsed("amount", String.valueOf(plugin.schedulers().size())),
                        Placeholder.unparsed("page", String.valueOf(menu.page + 1)),
                        Placeholder.unparsed("pages", String.valueOf(pages))));
        renderList(menu);
        player.openInventory(menu.inventory);
    }

    private void openEdit(@NotNull Player player, @NotNull ScheduleDraft draft) {
        EditMenu menu = new EditMenu(draft);
        Component title = draft.id == null
                ? plugin.messages().get("scheduler.edit.title-new")
                : plugin.messages().get("scheduler.edit.title", Placeholder.unparsed("id", draft.id));
        menu.inventory = plugin.getServer().createInventory(menu, SIZE, title);
        renderEdit(menu);
        player.openInventory(menu.inventory);
    }

    private void renderList(@NotNull ListMenu menu) {
        Inventory inventory = menu.inventory;
        inventory.clear();
        List<Schedule> all = plugin.schedulers().all();
        LocalDateTime now = LocalDateTime.now();
        int offset = menu.page * PAGE_SIZE;
        for (int index = 0; index < PAGE_SIZE && offset + index < all.size(); index++) {
            inventory.setItem(index, entryItem(all.get(offset + index), now));
        }
        fillBottom(inventory);
        if (menu.page > 0) {
            inventory.setItem(LIST_PREVIOUS, item(Material.ARROW, "scheduler.previous"));
        }
        if (offset + PAGE_SIZE < all.size()) {
            inventory.setItem(LIST_NEXT, item(Material.ARROW, "scheduler.next"));
        }
        inventory.setItem(LIST_ADD, item(Material.EMERALD, "scheduler.add"));
    }

    private void renderEdit(@NotNull EditMenu menu) {
        Inventory inventory = menu.inventory;
        ScheduleDraft draft = menu.draft;
        inventory.clear();

        LocalDateTime next = CronExpression.isValid(draft.cron())
                ? draft.expression().nextAfter(LocalDateTime.now())
                : null;
        inventory.setItem(EDIT_PREVIEW, item(Material.CLOCK,
                draft.customCron == null ? "scheduler.edit.preview" : "scheduler.edit.preview-custom",
                Placeholder.unparsed("cron", draft.cron()),
                Placeholder.unparsed("next", draft.enabled && next != null ? NEXT_RUN.format(next) : "never")));

        inventory.setItem(EDIT_KOTH, item(Material.BEACON, "scheduler.edit.koth",
                Placeholder.unparsed("value", draft.kothName)));
        inventory.setItem(EDIT_ACTION, item(
                draft.action == ScheduleAction.START ? Material.EMERALD_BLOCK : Material.REDSTONE_BLOCK,
                "scheduler.edit.action",
                Placeholder.unparsed("value", draft.action.name())));
        inventory.setItem(EDIT_FREQUENCY, item(Material.COMPARATOR, "scheduler.edit.frequency",
                Placeholder.parsed("value", plugin.messages().raw(
                        "scheduler.frequency." + draft.frequency.name().toLowerCase(java.util.Locale.ROOT)))));
        inventory.setItem(EDIT_ENABLED, item(draft.enabled ? Material.LIME_DYE : Material.GRAY_DYE,
                "scheduler.edit.enabled",
                Placeholder.unparsed("value", draft.enabled ? "enabled" : "disabled")));

        switch (draft.frequency) {
            case DAILY -> {
                inventory.setItem(EDIT_HOUR, item(Material.SUNFLOWER, "scheduler.edit.hour",
                        Placeholder.unparsed("value", twoDigits(draft.hour))));
                inventory.setItem(EDIT_MINUTE, item(Material.CLOCK, "scheduler.edit.minute",
                        Placeholder.unparsed("value", twoDigits(draft.minute))));
            }
            case EVERY_HOURS -> {
                inventory.setItem(EDIT_HOUR, item(Material.REPEATER, "scheduler.edit.interval-hours",
                        Placeholder.unparsed("value", String.valueOf(draft.interval))));
                inventory.setItem(EDIT_MINUTE, item(Material.CLOCK, "scheduler.edit.minute",
                        Placeholder.unparsed("value", twoDigits(draft.minute))));
            }
            case EVERY_MINUTES -> inventory.setItem(EDIT_HOUR, item(Material.REPEATER,
                    "scheduler.edit.interval-minutes",
                    Placeholder.unparsed("value", String.valueOf(draft.interval))));
        }

        inventory.setItem(EDIT_ALL_DAYS, item(draft.allDays() ? Material.LIME_DYE : Material.GRAY_DYE,
                "scheduler.edit.all-days",
                Placeholder.unparsed("value", draft.allDays() ? "enabled" : "disabled")));
        List<String> dayNames = plugin.messages().rawList("scheduler.edit.day-names");
        for (int index = 0; index < DAY_ORDER.length; index++) {
            boolean selected = draft.days[DAY_ORDER[index]];
            String name = index < dayNames.size() ? dayNames.get(index) : String.valueOf(index + 1);
            inventory.setItem(EDIT_FIRST_DAY + index, item(
                    selected ? Material.LIME_STAINED_GLASS_PANE : Material.GRAY_STAINED_GLASS_PANE,
                    "scheduler.edit.day",
                    Placeholder.parsed("day", name),
                    Placeholder.unparsed("value", selected ? "enabled" : "disabled")));
        }

        fillBottom(inventory);
        inventory.setItem(EDIT_BACK, item(Material.ARROW, "scheduler.edit.back"));
        inventory.setItem(EDIT_SAVE, item(Material.EMERALD, "scheduler.edit.save"));
        if (draft.id != null) {
            inventory.setItem(EDIT_DELETE, item(Material.BARRIER, "scheduler.edit.delete"));
        }
    }

    private void fillBottom(@NotNull Inventory inventory) {
        Menus.fill(inventory, BOTTOM_ROW, SIZE);
    }

    private @NotNull ItemStack entryItem(@NotNull Schedule schedule, @NotNull LocalDateTime now) {
        LocalDateTime next = schedule.nextRun(now);
        ItemStack stack = new ItemStack(schedule.enabled() ? Material.CLOCK : Material.GRAY_DYE);
        ItemMeta meta = stack.getItemMeta();
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

    private @NotNull ItemStack item(@NotNull Material material, @NotNull String key, TagResolver... resolvers) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        meta.displayName(plugin.messages().get(key + ".name", resolvers));
        meta.lore(plugin.messages().list(null, key + ".lore", resolvers));
        stack.setItemMeta(meta);
        return stack;
    }

    private static @NotNull String twoDigits(int value) {
        return value < 10 ? "0" + value : String.valueOf(value);
    }

    @EventHandler
    public void onDrag(@NotNull InventoryDragEvent event) {
        InventoryHolder holder = event.getView().getTopInventory().getHolder();
        if (holder instanceof ListMenu || holder instanceof EditMenu) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onClick(@NotNull InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        Inventory top = event.getView().getTopInventory();
        InventoryHolder holder = top.getHolder();
        if (!(holder instanceof ListMenu) && !(holder instanceof EditMenu)) {
            return;
        }
        event.setCancelled(true);
        if (event.getClickedInventory() == null || !event.getClickedInventory().equals(top)) {
            return;
        }
        if (holder instanceof ListMenu menu) {
            clickList(player, menu, event.getSlot(), event.getClick());
        } else {
            clickEdit(player, (EditMenu) holder, event.getSlot(), event.getClick());
        }
    }

    private void clickList(@NotNull Player player, @NotNull ListMenu menu, int slot, @NotNull ClickType click) {
        List<Schedule> all = plugin.schedulers().all();
        int offset = menu.page * PAGE_SIZE;
        switch (slot) {
            case LIST_PREVIOUS -> {
                if (menu.page > 0) {
                    openList(player, menu.page - 1);
                }
                return;
            }
            case LIST_NEXT -> {
                if (offset + PAGE_SIZE < all.size()) {
                    openList(player, menu.page + 1);
                }
                return;
            }
            case LIST_ADD -> {
                List<String> names = plugin.koths().names();
                if (names.isEmpty()) {
                    player.sendMessage(plugin.messages().get(player, "scheduler.no-koths"));
                    return;
                }
                openEdit(player, ScheduleDraft.fresh(names.get(0)));
                return;
            }
            default -> {
            }
        }
        if (slot >= PAGE_SIZE || offset + slot >= all.size()) {
            return;
        }
        Schedule schedule = all.get(offset + slot);
        switch (click) {
            case LEFT, SHIFT_LEFT -> openEdit(player, ScheduleDraft.of(schedule));
            case RIGHT -> {
                schedule.enabled(!schedule.enabled());
                plugin.schedulers().save();
                renderList(menu);
            }
            case SHIFT_RIGHT -> {
                plugin.schedulers().remove(schedule.id());
                player.sendMessage(plugin.messages().get(player, "scheduler.deleted",
                        Placeholder.unparsed("id", schedule.id())));
                openList(player, menu.page);
            }
            default -> {
            }
        }
    }

    private void clickEdit(@NotNull Player player, @NotNull EditMenu menu, int slot, @NotNull ClickType click) {
        ScheduleDraft draft = menu.draft;
        boolean forward = click == ClickType.LEFT || click == ClickType.SHIFT_LEFT;
        boolean shift = click.isShiftClick();

        switch (slot) {
            case EDIT_KOTH -> cycleKoth(draft, forward);
            case EDIT_ACTION -> draft.action = draft.action == ScheduleAction.START
                    ? ScheduleAction.STOP
                    : ScheduleAction.START;
            case EDIT_FREQUENCY -> {
                ScheduleDraft.Frequency[] values = ScheduleDraft.Frequency.values();
                int index = Math.floorMod(draft.frequency.ordinal() + (forward ? 1 : -1), values.length);
                draft.frequency = values[index];
                draft.interval = Math.min(draft.interval, draft.maxInterval());
                draft.touchTime();
            }
            case EDIT_ENABLED -> draft.enabled = !draft.enabled;
            case EDIT_HOUR -> {
                int step = (forward ? 1 : -1) * (shift ? 5 : 1);
                if (draft.frequency == ScheduleDraft.Frequency.DAILY) {
                    draft.hour = Math.floorMod(draft.hour + (forward ? 1 : -1) * (shift ? 6 : 1), 24);
                } else {
                    draft.interval = Math.max(1, Math.min(draft.maxInterval(), draft.interval + step));
                }
                draft.touchTime();
            }
            case EDIT_MINUTE -> {
                if (draft.frequency == ScheduleDraft.Frequency.EVERY_MINUTES) {
                    return;
                }
                draft.minute = Math.floorMod(draft.minute + (forward ? 1 : -1) * (shift ? 15 : 1), 60);
                draft.touchTime();
            }
            case EDIT_ALL_DAYS -> {
                java.util.Arrays.fill(draft.days, !draft.allDays());
                draft.touchTime();
            }
            case EDIT_BACK -> {
                openList(player, 0);
                return;
            }
            case EDIT_SAVE -> {
                save(player, draft);
                return;
            }
            case EDIT_DELETE -> {
                if (draft.id != null) {
                    plugin.schedulers().remove(draft.id);
                    player.sendMessage(plugin.messages().get(player, "scheduler.deleted",
                            Placeholder.unparsed("id", draft.id)));
                    openList(player, 0);
                }
                return;
            }
            default -> {
                int day = slot - EDIT_FIRST_DAY;
                if (day < 0 || day >= DAY_ORDER.length) {
                    return;
                }
                draft.days[DAY_ORDER[day]] = !draft.days[DAY_ORDER[day]];
                draft.touchTime();
            }
        }
        renderEdit(menu);
    }

    private void cycleKoth(@NotNull ScheduleDraft draft, boolean forward) {
        List<String> names = plugin.koths().names();
        if (names.isEmpty()) {
            return;
        }
        int current = -1;
        for (int index = 0; index < names.size(); index++) {
            if (names.get(index).equalsIgnoreCase(draft.kothName)) {
                current = index;
                break;
            }
        }
        int next = current < 0 ? 0 : Math.floorMod(current + (forward ? 1 : -1), names.size());
        draft.kothName = names.get(next);
    }

    private void save(@NotNull Player player, @NotNull ScheduleDraft draft) {
        if (draft.customCron == null && draft.noDays()) {
            player.sendMessage(plugin.messages().get(player, "scheduler.no-days"));
            return;
        }
        if (plugin.koths().get(draft.kothName) == null) {
            player.sendMessage(plugin.messages().get(player, "command.not-configured",
                    Placeholder.unparsed("koth", draft.kothName)));
            return;
        }
        if (!CronExpression.isValid(draft.cron())) {
            player.sendMessage(plugin.messages().get(player, "scheduler.bad-cron",
                    Placeholder.unparsed("cron", draft.cron())));
            return;
        }
        CronExpression cron = draft.expression();
        Schedule schedule = draft.id == null ? null : plugin.schedulers().get(draft.id);
        if (schedule == null) {
            schedule = plugin.schedulers().add(draft.kothName, cron, draft.action);
        } else {
            schedule.kothName(draft.kothName);
            schedule.cron(cron);
            schedule.action(draft.action);
        }
        schedule.enabled(draft.enabled);
        plugin.schedulers().save();

        LocalDateTime next = schedule.nextRun(LocalDateTime.now());
        player.sendMessage(plugin.messages().get(player, "scheduler.saved",
                Placeholder.unparsed("id", schedule.id()),
                Placeholder.unparsed("koth", schedule.kothName()),
                Placeholder.unparsed("cron", cron.raw()),
                Placeholder.unparsed("next", next == null ? "never" : NEXT_RUN.format(next))));
        openList(player, 0);
    }
}
