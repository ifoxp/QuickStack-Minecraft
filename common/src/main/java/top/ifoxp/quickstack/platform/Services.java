package top.ifoxp.quickstack.platform;

import java.util.ServiceLoader;
import top.ifoxp.quickstack.QuickStack;
import top.ifoxp.quickstack.platform.services.IClientPlatformHelper;
import top.ifoxp.quickstack.platform.services.IPlatformHelper;

/**
 * Finds the loader-specific implementations through {@link ServiceLoader}. Each loader module lists its
 * implementation in {@code META-INF/services}.
 */
public final class Services {

    public static final IPlatformHelper PLATFORM = load(IPlatformHelper.class);

    private static IClientPlatformHelper client;

    private Services() {
    }

    /** Loaded lazily so a dedicated server never touches client classes. */
    public static IClientPlatformHelper client() {
        if (client == null) {
            client = load(IClientPlatformHelper.class);
        }
        return client;
    }

    public static <T> T load(Class<T> clazz) {
        T service = ServiceLoader.load(clazz, Services.class.getClassLoader())
                .findFirst()
                .orElseThrow(() -> new NullPointerException("Failed to load service for " + clazz.getName()));
        QuickStack.LOG.debug("Loaded {} for service {}", service, clazz);
        return service;
    }
}
