package dev.d4nilpzz.nzKoth.koth;

import dev.d4nilpzz.nzKoth.util.Cuboid;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.Locale;

public final class Koth {

    private final String name;
    private KothMode mode;
    private Cuboid region;
    private Location spawn;

    private Duration captureTime = Duration.ofSeconds(300);
    private Duration maxTime = Duration.ofMinutes(15);
    private int targetScore = 300;
    private boolean resetOnLeave = true;
    private boolean teamMode = false;

    private boolean bossBarEnabled = true;
    private boolean scoreboardEnabled = true;
    private boolean broadcastEnabled = true;
    private int viewDistance = -1;
    private boolean sameWorldOnly = false;

    private boolean crownEnabled = true;
    private String crownMaterial = "GOLDEN_HELMET";
    private int crownCustomModelData = 1002;
    private double crownHeight = 2.2;
    private int crownSpinTicks = 40;
    private boolean crownGlow = true;
    private boolean crownGlint = false;
    private boolean crownOutline = true;
    private String crownOutlineColor = "gold";

    private final Rewards rewards = new Rewards();

    public Koth(@NotNull String name, @NotNull KothMode mode) {
        this.name = name;
        this.mode = mode;
    }

    public @NotNull String name() {
        return name;
    }

    public @NotNull String lowerName() {
        return name.toLowerCase(Locale.ROOT);
    }

    public @NotNull KothMode mode() {
        return mode;
    }

    public void mode(@NotNull KothMode value) {
        this.mode = value;
    }

    public @Nullable Cuboid region() {
        return region;
    }

    public void region(@Nullable Cuboid value) {
        this.region = value;
    }

    public boolean isConfigured() {
        return region != null;
    }

    public @Nullable Location spawn() {
        return spawn == null && region != null ? region.center() : spawn;
    }

    public void spawn(@Nullable Location value) {
        this.spawn = value;
    }

    public @NotNull Duration captureTime() {
        return captureTime;
    }

    public void captureTime(@NotNull Duration value) {
        this.captureTime = value;
    }

    public @NotNull Duration maxTime() {
        return maxTime;
    }

    public void maxTime(@NotNull Duration value) {
        this.maxTime = value;
    }

    public int targetScore() {
        return targetScore;
    }

    public void targetScore(int value) {
        this.targetScore = value;
    }

    public boolean resetOnLeave() {
        return resetOnLeave;
    }

    public void resetOnLeave(boolean value) {
        this.resetOnLeave = value;
    }

    public boolean teamMode() {
        return teamMode;
    }

    public void teamMode(boolean value) {
        this.teamMode = value;
    }

    public boolean bossBarEnabled() {
        return bossBarEnabled;
    }

    public void bossBarEnabled(boolean value) {
        this.bossBarEnabled = value;
    }

    public boolean scoreboardEnabled() {
        return scoreboardEnabled;
    }

    public void scoreboardEnabled(boolean value) {
        this.scoreboardEnabled = value;
    }

    public boolean broadcastEnabled() {
        return broadcastEnabled;
    }

    public void broadcastEnabled(boolean value) {
        this.broadcastEnabled = value;
    }

    public int viewDistance() {
        return viewDistance;
    }

    public void viewDistance(int value) {
        this.viewDistance = value;
    }

    public boolean sameWorldOnly() {
        return sameWorldOnly;
    }

    public void sameWorldOnly(boolean value) {
        this.sameWorldOnly = value;
    }

    public boolean crownEnabled() {
        return crownEnabled;
    }

    public void crownEnabled(boolean value) {
        this.crownEnabled = value;
    }

    public @NotNull String crownMaterial() {
        return crownMaterial;
    }

    public void crownMaterial(@NotNull String value) {
        this.crownMaterial = value;
    }

    public int crownCustomModelData() {
        return crownCustomModelData;
    }

    public void crownCustomModelData(int value) {
        this.crownCustomModelData = value;
    }

    public double crownHeight() {
        return crownHeight;
    }

    public void crownHeight(double value) {
        this.crownHeight = value;
    }

    public int crownSpinTicks() {
        return crownSpinTicks;
    }

    public void crownSpinTicks(int value) {
        this.crownSpinTicks = value;
    }

    public boolean crownGlow() {
        return crownGlow;
    }

    public void crownGlow(boolean value) {
        this.crownGlow = value;
    }

    public boolean crownGlint() {
        return crownGlint;
    }

    public void crownGlint(boolean value) {
        this.crownGlint = value;
    }

    public boolean crownOutline() {
        return crownOutline;
    }

    public void crownOutline(boolean value) {
        this.crownOutline = value;
    }

    public @NotNull String crownOutlineColor() {
        return crownOutlineColor;
    }

    public void crownOutlineColor(@NotNull String value) {
        this.crownOutlineColor = value;
    }

    public @NotNull Rewards rewards() {
        return rewards;
    }
}
