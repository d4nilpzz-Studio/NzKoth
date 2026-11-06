package dev.d4nilpzz.nzKoth.display;

import dev.d4nilpzz.nzKoth.NzKoth;
import dev.d4nilpzz.nzKoth.display.menu.BaseMenu;
import dev.d4nilpzz.nzKoth.koth.ItemReward;
import dev.d4nilpzz.nzKoth.koth.Koth;
import dev.d4nilpzz.nzKoth.text.Text;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ItemRewardEditor {

    private static final Pattern RANGE = Pattern.compile("(\\d+)\\s*(?:-\\s*(\\d+))?");
    private static final int STEP = 1;
    private static final int SHIFT_STEP = 10;

    private final NzKoth plugin;

    public ItemRewardEditor(@NotNull NzKoth plugin) {
        this.plugin = plugin;
    }

    public void open(@NotNull Player player, @NotNull Koth koth, int index) {
        if (index < 0 || index >= koth.rewards().items().size()) {
            plugin.rewardsEditor().open(player, koth);
            return;
        }
        new RewardMenu(koth, index).open(player);
    }

    private final class RewardMenu extends BaseMenu {

        private final Koth koth;
        private final int index;

        private RewardMenu(@NotNull Koth koth, int index) {
            super(ItemRewardEditor.this.plugin, ItemRewardEditor.this.plugin.messages().get("item-editor.title",
                    Placeholder.unparsed("koth", koth.name()),
                    Placeholder.unparsed("number", String.valueOf(index + 1))));
            this.koth = koth;
            this.index = index;
        }

        private @Nullable ItemReward reward() {
            List<ItemReward> items = koth.rewards().items();
            return index < items.size() ? items.get(index) : null;
        }

        private void update(@NotNull ItemReward value) {
            List<ItemReward> items = koth.rewards().items();
            if (index < items.size()) {
                items.set(index, value);
                plugin.koths().save(koth);
            }
        }

        private void saveAndRefresh(@NotNull Player player, @NotNull ItemReward value) {
            update(value);
            click(player);
            refresh();
        }

        @Override
        protected void render() {
            ItemReward reward = reward();
            if (reward == null) {
                return;
            }
            renderPreview(reward);
            renderChance(reward);
            renderAmount(reward);
            renderMessage(reward);

            backButton(45, (player, click, cursor) -> plugin.rewardsEditor().open(player, koth));
            set(53, icon(Material.LAVA_BUCKET, "item-editor.delete"), (player, click, cursor) -> {
                if (!click.isShiftClick()) {
                    player.sendMessage(plugin.messages().get(player, "rewards.shift-confirm"));
                    deny(player);
                    return;
                }
                List<ItemReward> items = koth.rewards().items();
                if (index < items.size()) {
                    items.remove(index);
                    plugin.koths().save(koth);
                }
                deny(player);
                plugin.rewardsEditor().open(player, koth);
            });
        }

        private void renderPreview(@NotNull ItemReward reward) {
            ItemStack preview = withLore(reward.item(), plugin.messages().list(null, "item-editor.preview.lore"));
            set(13, preview, (player, click, cursor) -> {
                if (cursor != null) {
                    update(reward.withItem(cursor));
                    success(player);
                    refresh();
                }
            });
        }

        private void renderChance(@NotNull ItemReward reward) {
            double chance = reward.chance();
            chanceButton(19, Material.RED_CONCRETE, "item-editor.chance-minus-ten", reward, -10);
            chanceButton(20, Material.RED_TERRACOTTA, "item-editor.chance-minus-one", reward, -1);
            chanceButton(22, Material.LIME_TERRACOTTA, "item-editor.chance-plus-one", reward, 1);
            chanceButton(23, Material.LIME_CONCRETE, "item-editor.chance-plus-ten", reward, 10);

            set(21, icon(Material.EXPERIENCE_BOTTLE, (int) Math.max(1, Math.round(chance)), "item-editor.chance",
                            Placeholder.unparsed("chance", reward.chanceText())),
                    (player, click, cursor) -> ask(player, "item-editor.prompt-chance", text -> {
                        try {
                            double value = Double.parseDouble(text.replace("%", "").replace(",", ".").trim());
                            if (Double.isNaN(value)) {
                                return null;
                            }
                            return reward.withChance(value);
                        } catch (NumberFormatException exception) {
                            return null;
                        }
                    }));
        }

        private void chanceButton(int slot, @NotNull Material material, @NotNull String key,
                                  @NotNull ItemReward reward, double step) {
            set(slot, icon(material, key, Placeholder.unparsed("chance", reward.chanceText())),
                    (player, click, cursor) -> {
                        double delta = click.isShiftClick() && Math.abs(step) == 1 ? step / 10 : step;
                        saveAndRefresh(player, reward.withChance(reward.chance() + delta));
                    });
        }

        private void renderAmount(@NotNull ItemReward reward) {
            int min = reward.minAmount();
            int max = reward.maxAmount();

            set(28, icon(Material.RED_TERRACOTTA, "item-editor.min-minus", steps()),
                    (player, click, cursor) -> saveAndRefresh(player,
                            reward.withAmount(min - step(click), max)));
            set(29, icon(Material.IRON_NUGGET, min, "item-editor.min",
                            Placeholder.unparsed("min", String.valueOf(min))),
                    (player, click, cursor) -> askNumber(player, "item-editor.prompt-min",
                            value -> reward.withAmount(value, Math.max(value, max))));
            set(30, icon(Material.LIME_TERRACOTTA, "item-editor.min-plus", steps()),
                    (player, click, cursor) -> {
                        int newMin = min + step(click);
                        saveAndRefresh(player, reward.withAmount(newMin, Math.max(newMin, max)));
                    });

            set(31, icon(Material.BUNDLE, "item-editor.amount",
                            Placeholder.unparsed("amount", reward.amountText())),
                    (player, click, cursor) -> ask(player, "item-editor.prompt-amount", text -> {
                        Matcher matcher = RANGE.matcher(text.trim());
                        if (!matcher.matches()) {
                            return null;
                        }
                        try {
                            int a = Integer.parseInt(matcher.group(1));
                            int b = matcher.group(2) == null ? a : Integer.parseInt(matcher.group(2));
                            return reward.withAmount(Math.min(a, b), Math.max(a, b));
                        } catch (NumberFormatException exception) {
                            return null;
                        }
                    }));

            set(32, icon(Material.RED_TERRACOTTA, "item-editor.max-minus", steps()),
                    (player, click, cursor) -> {
                        int newMax = Math.max(1, max - step(click));
                        saveAndRefresh(player, reward.withAmount(Math.min(min, newMax), newMax));
                    });
            set(33, icon(Material.GOLD_NUGGET, max, "item-editor.max",
                            Placeholder.unparsed("max", String.valueOf(max))),
                    (player, click, cursor) -> askNumber(player, "item-editor.prompt-max",
                            value -> reward.withAmount(Math.min(min, value), value)));
            set(34, icon(Material.LIME_TERRACOTTA, "item-editor.max-plus", steps()),
                    (player, click, cursor) -> saveAndRefresh(player,
                            reward.withAmount(min, max + step(click))));
        }

        private @NotNull TagResolver steps() {
            return TagResolver.resolver(
                    Placeholder.unparsed("step", String.valueOf(STEP)),
                    Placeholder.unparsed("shift-step", String.valueOf(SHIFT_STEP)));
        }

        private int step(@NotNull ClickType click) {
            return click.isShiftClick() ? SHIFT_STEP : STEP;
        }

        private void renderMessage(@NotNull ItemReward reward) {
            Component current = reward.message() == null
                    ? plugin.messages().get("item-editor.message-none")
                    : Text.parse(reward.message());
            set(25, icon(Material.OAK_SIGN, "item-editor.message", Placeholder.component("message", current)),
                    (player, click, cursor) -> {
                        if (click.isRightClick()) {
                            saveAndRefresh(player, reward.withMessage(null));
                            return;
                        }
                        ask(player, "item-editor.prompt-message", reward::withMessage);
                    });
        }

        private void askNumber(@NotNull Player player, @NotNull String key, @NotNull IntFunction<ItemReward> apply) {
            ask(player, key, text -> {
                try {
                    return apply.apply(Integer.parseInt(text.trim()));
                } catch (NumberFormatException exception) {
                    return null;
                }
            });
        }

        private void ask(@NotNull Player player, @NotNull String key, @NotNull Function<String, ItemReward> apply) {
            click(player);
            plugin.chatInput().ask(player, plugin.messages().get(player, key), text -> {
                ItemReward updated = apply.apply(text);
                if (updated != null) {
                    update(updated);
                    success(player);
                } else {
                    player.sendMessage(plugin.messages().get(player, "item-editor.invalid"));
                    deny(player);
                }
                ItemRewardEditor.this.open(player, koth, index);
            }, () -> ItemRewardEditor.this.open(player, koth, index));
        }

        @Override
        public void handleInventoryClick(@NotNull Player player, @NotNull ItemStack clicked, @NotNull ClickType click) {
            ItemReward reward = reward();
            if (reward != null) {
                update(reward.withItem(clicked));
                success(player);
                refresh();
            }
        }
    }
}
