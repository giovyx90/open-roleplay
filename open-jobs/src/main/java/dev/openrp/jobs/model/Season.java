package dev.openrp.jobs.model;

import java.time.LocalDate;
import java.util.Locale;

/**
 * The four seasons used by the optional seasonal pay multiplier. Setting-neutral: the multipliers
 * themselves live in config; this enum only names the slots and maps a real-world month onto one.
 */
public enum Season {

    SPRING,
    SUMMER,
    AUTUMN,
    WINTER;

    /** Northern-hemisphere month-to-season mapping, used when the seasonal source is real time. */
    public static Season fromMonth(int month) {
        return switch (month) {
            case 3, 4, 5 -> SPRING;
            case 6, 7, 8 -> SUMMER;
            case 9, 10, 11 -> AUTUMN;
            default -> WINTER;
        };
    }

    public static Season currentReal() {
        return fromMonth(LocalDate.now().getMonthValue());
    }

    public String configKey() {
        return name().toLowerCase(Locale.ROOT);
    }
}
