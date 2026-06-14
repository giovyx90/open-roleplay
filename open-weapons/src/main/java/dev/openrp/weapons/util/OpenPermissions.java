package dev.openrp.weapons.util;

import org.bukkit.command.CommandSender;

public final class OpenPermissions {
    private OpenPermissions() {
    }

    public static boolean hasAny(CommandSender sender, String... permissions) {
        if (sender == null) {
            return false;
        }
        if (sender.isOp()) {
            return true;
        }
        if (permissions == null) {
            return false;
        }
        for (String permission : permissions) {
            if (permission != null && !permission.isBlank() && sender.hasPermission(permission)) {
                return true;
            }
        }
        return false;
    }

    public static final class Weapons {
        public static final String VIEW = "openrp.weapons.view";
        public static final String GIVE = "openrp.weapons.give";
        public static final String DEBUG = "openrp.weapons.debug";
        public static final String ADMIN = "openrp.weapons.admin";

        private Weapons() {
        }
    }

    public static final class Staff {
        public static final String ADMIN = "openrp.staff.admin";
        public static final String RELOAD = "openrp.staff.reload";

        private Staff() {
        }
    }

    public static final class Test {
        public static final String DEBUG = "openrp.test.debug";
        public static final String ITEMS = "openrp.test.items";

        private Test() {
        }
    }

    public static final class Robbery {
        public static final String ADMIN = "openrp.robbery.admin";

        private Robbery() {
        }
    }

    public static final class Utility {
        public static final String ADMIN = "openrp.utility.admin";

        private Utility() {
        }
    }

    public static final class Build {
        public static final String TOOLS = "openrp.build.tools";

        private Build() {
        }
    }
}
