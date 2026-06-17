package dev.openrp.crime.integration;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Optional, fully reflective registration of Open Crime as an OpenCore module, mirroring the pattern
 * the other Open Roleplay modules use. Open Crime manages its own JavaPlugin lifecycle, so the
 * registered module's callbacks are no-ops - registering just makes the module visible to OpenCore.
 * The plugin runs perfectly standalone; {@link #register()} returns {@code false} when OpenCore is
 * absent and nothing else depends on it.
 */
public final class OpenCoreModuleRegistration {

    private static final String MODULE_ID = "open-crime";

    private final JavaPlugin plugin;
    private Object openCore;
    private Class<?> coreClass;
    private Class<?> moduleClass;
    private Object moduleProxy;

    public OpenCoreModuleRegistration(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean register() {
        Plugin openCorePlugin = plugin.getServer().getPluginManager().getPlugin("OpenCore");
        if (openCorePlugin == null || !openCorePlugin.isEnabled()) {
            return false;
        }
        try {
            ClassLoader loader = openCorePlugin.getClass().getClassLoader();
            coreClass = Class.forName("dev.openrp.core.api.OpenRoleplayCore", true, loader);
            moduleClass = Class.forName("dev.openrp.core.api.module.OpenModule", true, loader);
            @SuppressWarnings({"rawtypes", "unchecked"})
            RegisteredServiceProvider<?> registration =
                    plugin.getServer().getServicesManager().getRegistration((Class) coreClass);
            if (registration == null || registration.getProvider() == null) {
                return false;
            }
            openCore = registration.getProvider();
            moduleProxy = Proxy.newProxyInstance(loader, new Class<?>[]{moduleClass}, moduleHandler());
            Object modules = coreClass.getMethod("modules").invoke(openCore);
            modules.getClass().getMethod("register", moduleClass).invoke(modules, moduleProxy);
            plugin.getLogger().info("[OpenCrime] Registered in OpenCore.");
            return true;
        } catch (ReflectiveOperationException | LinkageError error) {
            plugin.getLogger().warning("[OpenCrime] OpenCore found, but module registration failed: "
                    + rootMessage(error));
            reset();
            return false;
        }
    }

    public void unregister() {
        if (openCore == null || coreClass == null) {
            return;
        }
        try {
            Object modules = coreClass.getMethod("modules").invoke(openCore);
            modules.getClass().getMethod("unregister", String.class).invoke(modules, MODULE_ID);
        } catch (ReflectiveOperationException | LinkageError error) {
            plugin.getLogger().warning("[OpenCrime] Unregister from OpenCore failed: " + rootMessage(error));
        } finally {
            reset();
        }
    }

    private InvocationHandler moduleHandler() {
        return (proxy, method, args) -> switch (method.getName()) {
            case "id" -> MODULE_ID;
            case "onEnable", "onDisable", "onReload" -> null;
            case "toString" -> "OpenCrimeModule";
            case "hashCode" -> System.identityHashCode(proxy);
            case "equals" -> proxy == (args == null || args.length == 0 ? null : args[0]);
            default -> defaultValue(method);
        };
    }

    private void reset() {
        openCore = null;
        moduleProxy = null;
        coreClass = null;
        moduleClass = null;
    }

    private Object defaultValue(Method method) {
        Class<?> returnType = method.getReturnType();
        if (returnType == boolean.class) {
            return false;
        }
        if (returnType == byte.class || returnType == short.class || returnType == int.class || returnType == long.class) {
            return 0;
        }
        if (returnType == float.class || returnType == double.class) {
            return 0.0;
        }
        if (returnType == char.class) {
            return '\0';
        }
        return null;
    }

    private String rootMessage(Throwable error) {
        Throwable cause = error.getCause() == null ? error : error.getCause();
        return cause.getMessage() == null ? cause.getClass().getSimpleName() : cause.getMessage();
    }
}
