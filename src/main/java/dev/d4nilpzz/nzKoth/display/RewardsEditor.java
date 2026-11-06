package dev.d4nilpzz.nzKoth.display;

import dev.d4nilpzz.nzKoth.NzKoth;
import dev.d4nilpzz.nzKoth.display.menu.BaseMenu;
import dev.d4nilpzz.nzKoth.koth.ItemReward;
import dev.d4nilpzz.nzKoth.koth.Koth;
import dev.d4nilpzz.nzKoth.text.Text;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class RewardsEditor {

    private static final int PER_PAGE = 27;
    private static final double DEFAULT_CHANCE = 100;
    private static final double CHANCE_STEP = 5;

    private enum Kind {
        COMMAND,
        BROADCAST,
        ITEM
    }

    private record Entry(@NotNull Kind kind, int index) {
    }

    private final NzKoth plugin;

    public RewardsEditor(@NotNull NzKoth plugin) {
        this.plugin = plugin;
    }

    public void open(@NotNull Player player, @NotNull Koth koth) {
        open(player, koth, 0);
    }

    public void open(@NotNull Player player, @NotNull Koth koth, int page) {
        new LootMenu(koth, page).open(player);
    }

    private static @NotNull List<Entry> entries(@NotNull Koth koth) {
        List<Entry> out = new ArrayList<>();
        for (int index = 0; index < koth.rewards().commands().size(); index++) {
            out.add(new Entry(Kind.COMMAND, index));
        }
        for (int index = 0; index < koth.rewards().broadcastCommands().size(); index++) {
            out.add(new Entry(Kind.BROADCAST, index));
        }
        for (int index = 0; index < koth.rewards().items().size(); index++) {
            out.add(new Entry(Kind.ITEM, index));
        }
        return out;
    }

    private static @NotNull List<String> commands(@NotNull Koth koth, @NotNull Kind kind) {
        return kind == Kind.COMMAND ? koth.rewards().commands() : koth.rewards().broadcastCommands();
    }

    private void addItem(@NotNull Koth koth, @NotNull ItemStack item) {
        koth.rewards().items().add(new ItemReward(item, DEFAULT_CHANCE, item.getAmount(), item.getAmount(), null));
        plugin.koths().save(koth);
    }

    private final class LootMenu extends BaseMenu {

        private final Koth koth;
        private final int page;

        private LootMenu(@NotNull Koth koth, int page) {
            super(RewardsEditor.this.plugin, RewardsEditor.this.plugin.messages().get("rewards.title",
                    Placeholder.unparsed("koth", koth.name())));
            this.koth = koth;
            this.page = page;
        }

        @Override
        protected void render() {
            List<Entry> entries = entries(koth);
            int pages = Math.max(1, (entries.size() + 1 + PER_PAGE - 1) / PER_PAGE);
            int current = Math.max(0, Math.min(page, pages - 1));

            for (int i = 0; i < PER_PAGE; i++) {
                int index = current * PER_PAGE + i;
                if (index >= entries.size()) {
                    break;
                }
                Entry entry = entries.get(index);
                if (entry.kind() == Kind.ITEM) {
                    renderItem(i, entry.index());
                } else {
                    renderCommand(i, entry, current);
                }
            }

            backButton(36, (player, click, cursor) -> plugin.editor().open(player, koth));
            pageButton(37, "rewards.previous-page", current - 1, pages);

            set(39, icon(Material.COMMAND_BLOCK, "rewards.add-command"),
                    (player, click, cursor) -> askCommand(player, Kind.COMMAND, -1, current));

            double total = koth.rewards().items().stream().mapToDouble(ItemReward::chance).sum();
            set(40, icon(Material.KNOWLEDGE_BOOK, "rewards.help",
                    Placeholder.unparsed("rewards", String.valueOf(entries.size())),
                    Placeholder.unparsed("average", ItemReward.number(Math.round(total) / 100.0))));

            set(41, icon(Material.BELL, "rewards.add-broadcast"),
                    (player, click, cursor) -> askCommand(player, Kind.BROADCAST, -1, current));

            pageButton(43, "rewards.next-page", current + 1, pages);

            set(44, icon(Material.LAVA_BUCKET, "rewards.delete-all"), (player, click, cursor) -> {
                if (!click.isShiftClick()) {
                    player.sendMessage(plugin.messages().get(player, "rewards.shift-confirm"));
                    deny(player);
                    return;
                }
                koth.rewards().commands(List.of());
                koth.rewards().broadcastCommands(List.of());
                koth.rewards().items(List.of());
                plugin.koths().save(koth);
                deny(player);
                refresh();
            });
        }

        private void renderItem(int slot, int index) {
            ItemReward reward = koth.rewards().items().get(index);
            ItemStack base = reward.item().clone();
            base.setAmount(Math.max(1, Math.min(99, reward.minAmount())));
            List<Component> lore = new ArrayList<>(plugin.messages().list(null, "rewards.item.lore",
                    Placeholder.unparsed("chance", reward.chanceText()),
                    Placeholder.unparsed("amount", reward.amountText())));
            if (reward.message() != null) {
                lore.add(plugin.messages().get("rewards.item.message",
                        Placeholder.component("message", Text.parse(reward.message()))));
            }
            lore.addAll(plugin.messages().list(null, "rewards.item.controls"));
            set(slot, withLore(base, lore), (player, click, cursor) -> {
                List<ItemReward> items = koth.rewards().items();
                if (index >= items.size()) {
                    refresh();
                    return;
                }
                if (click == ClickType.SHIFT_RIGHT) {
                    items.remove(index);
                    plugin.koths().save(koth);
                    deny(player);
                    refresh();
                    return;
                }
                if (click == ClickType.RIGHT || click == ClickType.SHIFT_LEFT) {
                    double step = click == ClickType.RIGHT ? -CHANCE_STEP : CHANCE_STEP;
                    items.set(index, reward.withChance(reward.chance() + step));
                    plugin.koths().save(koth);
                    click(player);
                    refresh();
                    return;
                }
                click(player);
                plugin.itemRewardEditor().open(player, koth, index);
            });
        }

        private void renderCommand(int slot, @NotNull Entry entry, int current) {
            boolean broadcast = entry.kind() == Kind.BROADCAST;
            String command = commands(koth, entry.kind()).get(entry.index());
            String key = broadcast ? "rewards.broadcast-command" : "rewards.command";
            set(slot, icon(broadcast ? Material.BELL : Material.COMMAND_BLOCK, key,
                            Placeholder.unparsed("number", String.valueOf(entry.index() + 1)),
                            Placeholder.unparsed("command", command)),
                    (player, click, cursor) -> {
                        List<String> source = commands(koth, entry.kind());
                        if (entry.index() >= source.size()) {
                            refresh();
                            return;
                        }
                        if (click == ClickType.SHIFT_RIGHT) {
                            source.remove(entry.index());
                            plugin.koths().save(koth);
                            deny(player);
                            refresh();
                            return;
                        }
                        if (click == ClickType.RIGHT) {
                            source.remove(entry.index());
                            commands(koth, broadcast ? Kind.COMMAND : Kind.BROADCAST).add(command);
                            plugin.koths().save(koth);
                            click(player);
                            refresh();
                            return;
                        }
                        askCommand(player, entry.kind(), entry.index(), current);
                    });
        }

        private void pageButton(int slot, @NotNull String key, int target, int pages) {
            if (target < 0 || target >= pages) {
                return;
            }
            set(slot, icon(Material.SPECTRAL_ARROW, key,
                            Placeholder.unparsed("page", String.valueOf(target + 1)),
                            Placeholder.unparsed("pages", String.valueOf(pages))),
                    (player, click, cursor) -> {
                        click(player);
                        RewardsEditor.this.open(player, koth, target);
                    });
        }

        private void askCommand(@NotNull Player player, @NotNull Kind kind, int index, int current) {
            click(player);
            List<String> source = commands(koth, kind);
            String key = kind == Kind.BROADCAST ? "rewards.prompt-broadcast" : "rewards.prompt-command";
            plugin.chatInput().ask(player, plugin.messages().get(player, key), text -> {
                String command = text.startsWith("/") ? text.substring(1).trim() : text;
                List<String> target = commands(koth, kind);
                if (index >= 0 && index < target.size()) {
                    target.set(index, command);
                } else {
                    target.add(command);
                }
                plugin.koths().save(koth);
                success(player);
                RewardsEditor.this.open(player, koth, current);
            }, () -> RewardsEditor.this.open(player, koth, current));
            if (index >= 0 && index < source.size()) {
                String existing = source.get(index);
                player.sendMessage(plugin.messages().get(player, "rewards.current",
                                Placeholder.unparsed("command", existing))
                        .clickEvent(ClickEvent.suggestCommand(existing)));
            }
        }

        @Override
        protected boolean keepEmpty(int slot) {
            return slot < PER_PAGE;
        }

        @Override
        protected void handleEmptyClick(@NotNull Player player, int slot, @NotNull ClickType click,
                                        @Nullable ItemStack cursor) {
            if (slot < PER_PAGE && cursor != null) {
                addItem(koth, cursor);
                success(player);
                refresh();
            }
        }

        @Override
        public void handleInventoryClick(@NotNull Player player, @NotNull ItemStack clicked, @NotNull ClickType click) {
            addItem(koth, clicked);
            success(player);
            refresh();
        }
    }
}
