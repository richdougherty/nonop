// Copyright 2025 Rich Dougherty <rich@rd.nz>

package nz.rd.nonop.internal.model;

import java.util.Map;
import java.util.WeakHashMap;

public final class JVMRegistry {
    // TODO: Make a static class in each ClassLoader that holds this reference - classes can call directly without a Map; frees automatically when ClassLoader unloads
    private final Map<ClassLoader, ClassLoaderRegistry> classLoaderRegWeakRefs = new WeakHashMap<>();
    // TODO: Use a threadlocal cache of the last used WeakReference<ClassLoader> to avoid repeated lookups

    // TODO: Synchronized is slow; improve with a more efficient concurrent structure later

    public synchronized ClassLoaderRegistry getClassLoaderRegistry(ClassLoader classLoader) {
        // Check if the registry already exists
        ClassLoaderRegistry registry = classLoaderRegWeakRefs.get(classLoader);
        if (registry == null) {
            // Create a new registry and store it in the map
            registry = new ClassLoaderRegistry(classLoader);
            classLoaderRegWeakRefs.put(classLoader, registry);
        }
        return registry;
    }

}
