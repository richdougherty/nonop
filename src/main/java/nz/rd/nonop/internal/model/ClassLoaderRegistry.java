// Copyright 2025 Rich Dougherty <rich@rd.nz>

package nz.rd.nonop.internal.model;

import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class ClassLoaderRegistry {

    private final WeakReference<ClassLoader> classLoader;
    private final Map<Class<?>, ClassUsageState> classUsageStates = new ConcurrentHashMap<>();

    public ClassLoaderRegistry(ClassLoader classLoader) {
        this.classLoader = new WeakReference<>(classLoader);
    }

    public ClassUsageState getOrCreateClassUsageState(Class<?> clazz) {
        // Sanity check match the class loader of the class with this registry's class loader
        if (clazz.getClassLoader() != classLoader.get()) {
            throw new IllegalArgumentException("Class " + clazz.getName() + " does not belong to this ClassLoaderRegistry's class loader.");
        }

        return classUsageStates.computeIfAbsent(clazz, k -> new ClassUsageState(clazz));
    }
}
