package com.azure.pathogenesis.platform;

import com.azure.pathogenesis.platform.services.IPlatformHelper;
import com.azure.pathogenesis.platform.services.IRegistryHelper;

import java.util.ServiceLoader;

public final class Services {

    public static final IPlatformHelper PLATFORM = load(IPlatformHelper.class);

    public static final IRegistryHelper REGISTRY = load(IRegistryHelper.class);

    private Services() {}

    public static <T> T load(Class<T> clazz) {
        return ServiceLoader.load(clazz, Services.class.getClassLoader())
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("Failed to load service for " + clazz.getName()));
    }
}
