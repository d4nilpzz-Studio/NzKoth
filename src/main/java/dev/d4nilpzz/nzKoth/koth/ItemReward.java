package dev.d4nilpzz.nzKoth.koth;

import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.ThreadLocalRandom;

public record ItemReward(@NotNull ItemStack item, double chance, int minAmount, int maxAmount, @Nullable String message) {

    public static final double MAX_CHANCE = 100.0;
    public static final int MAX_AMOUNT = 2304;

    public ItemReward {
        item = item.clone();
        item.setAmount(1);
        chance = Math.round(Math.max(0.0, Math.min(MAX_CHANCE, chance)) * 100.0) / 100.0;
        minAmount = Math.max(1, Math.min(MAX_AMOUNT, minAmount));
        maxAmount = Math.max(minAmount, Math.min(MAX_AMOUNT, maxAmount));
        if (message != null && message.isBlank()) {
            message = null;
        }
    }

    public @NotNull ItemReward withItem(@NotNull ItemStack value) {
        return new ItemReward(value, chance, minAmount, maxAmount, message);
    }

    public @NotNull ItemReward withChance(double value) {
        return new ItemReward(item, value, minAmount, maxAmount, message);
    }

    public @NotNull ItemReward withAmount(int min, int max) {
        return new ItemReward(item, chance, min, max, message);
    }

    public @NotNull ItemReward withMessage(@Nullable String value) {
        return new ItemReward(item, chance, minAmount, maxAmount, value);
    }

    public boolean roll() {
        return chance >= MAX_CHANCE || ThreadLocalRandom.current().nextDouble(MAX_CHANCE) < chance;
    }

    public int rollAmount() {
        return minAmount == maxAmount ? minAmount : ThreadLocalRandom.current().nextInt(minAmount, maxAmount + 1);
    }

    public @NotNull String chanceText() {
        return number(chance);
    }

    public @NotNull String amountText() {
        return minAmount == maxAmount ? String.valueOf(minAmount) : minAmount + "-" + maxAmount;
    }

    public static @NotNull String number(double value) {
        if (value == Math.rint(value)) {
            return String.valueOf((long) value);
        }
        return String.valueOf(Math.round(value * 100.0) / 100.0);
    }
}
