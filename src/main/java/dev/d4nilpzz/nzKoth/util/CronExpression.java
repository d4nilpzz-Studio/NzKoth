package dev.d4nilpzz.nzKoth.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.BitSet;
import java.util.Locale;

public final class CronExpression {

    private static final String[] MONTH_NAMES = {
            "jan", "feb", "mar", "apr", "may", "jun", "jul", "aug", "sep", "oct", "nov", "dec"
    };
    private static final String[] DAY_NAMES = {
            "sun", "mon", "tue", "wed", "thu", "fri", "sat"
    };
    private static final int SEARCH_LIMIT_MINUTES = 60 * 24 * 366 * 4;

    private final String raw;
    private final BitSet minutes;
    private final BitSet hours;
    private final BitSet daysOfMonth;
    private final BitSet months;
    private final BitSet daysOfWeek;
    private final boolean dayOfMonthRestricted;
    private final boolean dayOfWeekRestricted;

    private CronExpression(String raw,
                           BitSet minutes,
                           BitSet hours,
                           BitSet daysOfMonth,
                           BitSet months,
                           BitSet daysOfWeek,
                           boolean dayOfMonthRestricted,
                           boolean dayOfWeekRestricted) {
        this.raw = raw;
        this.minutes = minutes;
        this.hours = hours;
        this.daysOfMonth = daysOfMonth;
        this.months = months;
        this.daysOfWeek = daysOfWeek;
        this.dayOfMonthRestricted = dayOfMonthRestricted;
        this.dayOfWeekRestricted = dayOfWeekRestricted;
    }

    public static @NotNull CronExpression parse(@NotNull String expression) {
        String trimmed = expression.trim().toLowerCase(Locale.ROOT);
        String[] parts = trimmed.split("\\s+");
        if (parts.length != 5) {
            throw new IllegalArgumentException(
                    "A cron expression needs 5 fields (minute hour day-of-month month day-of-week), got " + parts.length);
        }
        return new CronExpression(trimmed,
                field(parts[0], 0, 59, null),
                field(parts[1], 0, 23, null),
                field(parts[2], 1, 31, null),
                field(parts[3], 1, 12, MONTH_NAMES),
                field(parts[4], 0, 6, DAY_NAMES),
                !parts[2].equals("*"),
                !parts[4].equals("*"));
    }

    public static boolean isValid(@NotNull String expression) {
        try {
            parse(expression);
            return true;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private static @NotNull BitSet field(@NotNull String spec, int min, int max, String[] names) {
        BitSet bits = new BitSet(max + 1);
        for (String part : spec.split(",")) {
            parsePart(part, min, max, names, bits);
        }
        if (bits.isEmpty()) {
            throw new IllegalArgumentException("Field '" + spec + "' matches nothing");
        }
        return bits;
    }

    private static void parsePart(@NotNull String part, int min, int max, String[] names, @NotNull BitSet bits) {
        int step = 1;
        String range = part;
        int slash = part.indexOf('/');
        if (slash >= 0) {
            range = part.substring(0, slash);
            String stepText = part.substring(slash + 1);
            try {
                step = Integer.parseInt(stepText);
            } catch (NumberFormatException exception) {
                throw new IllegalArgumentException("Invalid step '" + stepText + "' in '" + part + "'");
            }
            if (step < 1) {
                throw new IllegalArgumentException("Step must be at least 1 in '" + part + "'");
            }
        }

        int from;
        int to;
        if (range.equals("*")) {
            from = min;
            to = max;
        } else {
            int dash = range.indexOf('-');
            if (dash > 0) {
                from = value(range.substring(0, dash), min, max, names);
                to = value(range.substring(dash + 1), min, max, names);
            } else {
                from = value(range, min, max, names);
                to = slash >= 0 ? max : from;
            }
        }
        if (from > to) {
            throw new IllegalArgumentException("Range start is after its end in '" + part + "'");
        }
        for (int current = from; current <= to; current += step) {
            bits.set(current);
        }
    }

    private static int value(@NotNull String token, int min, int max, String[] names) {
        String text = token.trim();
        if (names != null) {
            for (int index = 0; index < names.length; index++) {
                if (names[index].equals(text)) {
                    return min == 0 ? index : index + 1;
                }
            }
        }
        int parsed;
        try {
            parsed = Integer.parseInt(text);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Invalid value '" + token + "'");
        }
        if (parsed == 7 && max == 6) {
            parsed = 0;
        }
        if (parsed < min || parsed > max) {
            throw new IllegalArgumentException("Value '" + token + "' is outside " + min + "-" + max);
        }
        return parsed;
    }

    public boolean matches(@NotNull LocalDateTime time) {
        if (!minutes.get(time.getMinute())
                || !hours.get(time.getHour())
                || !months.get(time.getMonthValue())) {
            return false;
        }
        boolean dayOfMonthMatch = daysOfMonth.get(time.getDayOfMonth());
        boolean dayOfWeekMatch = daysOfWeek.get(time.getDayOfWeek().getValue() % 7);

        if (dayOfMonthRestricted && dayOfWeekRestricted) {
            return dayOfMonthMatch || dayOfWeekMatch;
        }
        return dayOfMonthMatch && dayOfWeekMatch;
    }

    public @Nullable LocalDateTime nextAfter(@NotNull LocalDateTime from) {
        LocalDateTime candidate = from.truncatedTo(ChronoUnit.MINUTES).plusMinutes(1);
        for (int step = 0; step < SEARCH_LIMIT_MINUTES; step++) {
            if (matches(candidate)) {
                return candidate;
            }
            candidate = candidate.plusMinutes(1);
        }
        return null;
    }

    public @NotNull String raw() {
        return raw;
    }

    @Override
    public String toString() {
        return raw;
    }
}
