package dev.openrp.fdo.core;

import dev.openrp.fdo.config.Corps;

/**
 * Formats dossier ids from the configured pattern. The default is
 * {@code {anno}/{numero}/{sigla_corpo}} (e.g. {@code 2026/1/PS}); the core never embeds an authority
 * name, only the configured short code, so the format stays setting-neutral.
 */
public final class DossierIds {

    private DossierIds() {
    }

    public static String format(String pattern, int year, long number, Corps corps) {
        String sigla = corps == null ? "" : corps.sigla();
        return (pattern == null ? "{anno}/{numero}/{sigla_corpo}" : pattern)
                .replace("{anno}", Integer.toString(year))
                .replace("{numero}", Long.toString(number))
                .replace("{sigla_corpo}", sigla);
    }

    /** Counter key for a year/corps pair. */
    public static String counterKey(int year, String corpsId) {
        return year + "/" + corpsId;
    }
}
