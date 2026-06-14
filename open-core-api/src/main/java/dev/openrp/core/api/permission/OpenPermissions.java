package dev.openrp.core.api.permission;

import org.bukkit.command.CommandSender;

public final class OpenPermissions {
    public static final String CORE_ADMIN = "openrp.core.admin";
    public static final String CORE_RELOAD = "openrp.core.reload";
    public static final String CORE_DEBUG = "openrp.core.debug";

    private OpenPermissions() {
    }

    public static boolean has(CommandSender sender, String permission) {
        return sender != null && (sender.isOp() || sender.hasPermission(permission));
    }

    public static boolean hasAny(CommandSender sender, String... permissions) {
        if (sender == null) {
            return false;
        }
        if (sender.isOp()) {
            return true;
        }
        for (String permission : permissions) {
            if (permission != null && sender.hasPermission(permission)) {
                return true;
            }
        }
        return false;
    }
}
