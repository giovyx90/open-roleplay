package dev.openrp.core.api.module;

import java.util.Map;
import java.util.Optional;

public interface OpenModuleManager {
    Map<String, OpenModule> registeredModules();

    OpenModuleState state(String id);

    Optional<String> lastError(String id);

    <T extends OpenModule> Optional<T> module(Class<T> type);
}
