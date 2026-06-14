package dev.openrp;

import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

final class OpenCoreModuleRegistration {
    private final JavaPlugin plugin;
    private final String moduleId;
    private Object openCore;
    private Object moduleProxy;
    private Class<?> coreClass;
    private Class<?> moduleClass;

    OpenCoreModuleRegistration(JavaPlugin plugin, String moduleId) {
        this.plugin = plugin;
        this.moduleId = moduleId;
    }

    boolean register() {
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
            plugin.getLogger().info("[OpenAccess] Registrato in OpenCore.");
            return true;
        } catch (ReflectiveOperationException | LinkageError error) {
            plugin.getLogger().warning("[OpenAccess] OpenCore trovato, ma registrazione modulo fallita: " + error.getMessage());
            openCore = null;
            moduleProxy = null;
            coreClass = null;
            moduleClass = null;
            return false;
        }
    }

    void unregister() {
        if (openCore == null || coreClass == null) {
            return;
        }
        try {
            Object modules = coreClass.getMethod("modules").invoke(openCore);
            modules.getClass().getMethod("unregister", String.class).invoke(modules, moduleId);
        } catch (ReflectiveOperationException | LinkageError error) {
            plugin.getLogger().warning("[OpenAccess] Unregister da OpenCore fallito: " + error.getMessage());
        } finally {
            openCore = null;
            moduleProxy = null;
            coreClass = null;
            moduleClass = null;
        }
    }

    private InvocationHandler moduleHandler() {
        return (proxy, method, args) -> switch (method.getName()) {
            case "id" -> moduleId;
            case "onEnable", "onDisable", "onReload" -> null;
            case "toString" -> "OpenAccessModule";
            default -> defaultValue(method);
        };
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
}
