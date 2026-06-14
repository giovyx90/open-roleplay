package dev.openrp.access;

import org.bukkit.command.CommandSender;

public final class AccessPermissions {
    private AccessPermissions() {
    }

    public static final String ADMIN = "openrp.access.admin";
    public static final String REGION_MANAGE = "openrp.access.region.manage";
    public static final String RELOAD = "openrp.access.reload";
    public static final String DEBUG = "openrp.access.debug";
    public static final String BYPASS = "openrp.access.bypass";

    public static final String LEGACY_ADMIN = "next.access.admin";
    public static final String LEGACY_REGION_MANAGE = "next.access.region.manage";
    public static final String LEGACY_RELOAD = "next.access.reload";
    public static final String LEGACY_DEBUG = "next.access.debug";
    public static final String LEGACY_BYPASS = "next.access.bypass";

    public static boolean hasAdmin(CommandSender sender) {
        return hasAny(sender, ADMIN, LEGACY_ADMIN);
    }

    public static boolean hasRegionManage(CommandSender sender) {
        return hasAny(sender, REGION_MANAGE, LEGACY_REGION_MANAGE) || hasAdmin(sender);
    }

    public static boolean hasReload(CommandSender sender) {
        return hasAny(sender, RELOAD, LEGACY_RELOAD) || hasAdmin(sender);
    }

    public static boolean hasDebug(CommandSender sender) {
        return hasAny(sender, DEBUG, LEGACY_DEBUG) || hasAdmin(sender);
    }

    public static boolean hasBypass(CommandSender sender) {
        return hasAny(sender, BYPASS, LEGACY_BYPASS) || hasAdmin(sender);
    }

    private static boolean hasAny(CommandSender sender, String... permissions) {
        if (sender == null || permissions == null) {
            return false;
        }
        for (String permission : permissions) {
            if (permission != null && sender.hasPermission(permission)) {
                return true;
            }
        }
        return false;
    }
}
