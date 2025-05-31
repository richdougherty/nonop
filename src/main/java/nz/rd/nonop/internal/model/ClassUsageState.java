// Copyright 2025 Rich Dougherty <rich@rd.nz>

package nz.rd.nonop.internal.model;

import java.lang.ref.WeakReference;
import java.util.HashSet;
import java.util.Set;

public final class ClassUsageState {
    // TODO: Optimize memory by omitting this field if can always be provided by caller
    private final WeakReference<? extends Class<?>> clazzWeakRef;
    // TODO: Optimize memory usage by using a more space-efficient structure
    private final Set<String> usedMethods = new HashSet<>();
    private boolean reinstrumentationScheduled = false;

    public ClassUsageState(Class<?> clazz /*, Collection<String> methodSignatures */) {
        this.clazzWeakRef = new WeakReference<>(clazz);
    }

    public WeakReference<? extends Class<?>> getClazzWeakRef() {
        return clazzWeakRef;
    }

    // TODO: Consider changing to avoid or delay reinstrumentation on first call (in case never needed again), but do immediately on second call
    public enum MarkResult {
        ALREADY_MARKED(false, false),
        NEWLY_MARKED_INSTRUMENTATION_NEEDED(true, true),
        NEWLY_MARKED_INSTRUMENTATION_ALREADY_SCHEDULED(true, false);

        final boolean added;
        final boolean instrumentationNeeded;

        MarkResult(boolean added, boolean instrumentationNeeded) {
            this.added = added;
            this.instrumentationNeeded = instrumentationNeeded;
        }

        public boolean isAdded() {
            return added;
        }

        public boolean isInstrumentationNeeded() {
            return instrumentationNeeded;
        }
    }

    // returns if needs reinstrumentation status newly set to true so reinstrumentation needs to be scheduled
    // TODO: Consider an enum so return value is easier to understand
    public synchronized MarkResult recordMethodUsedAndDecideIfInstrumentationNeeded(String methodSignature) {
        boolean added = usedMethods.add(methodSignature);
        if (added) {
            if (reinstrumentationScheduled) {
                return MarkResult.NEWLY_MARKED_INSTRUMENTATION_ALREADY_SCHEDULED;
            } else {
                reinstrumentationScheduled = true;
                return MarkResult.NEWLY_MARKED_INSTRUMENTATION_NEEDED;
            }
        } else {
            return MarkResult.ALREADY_MARKED;
        }
    }

    public synchronized Set<String> recordInstrumentationWithSnapshotOfUsage() {
        Set<String> usageSnapshot = new HashSet<>(usedMethods);
        reinstrumentationScheduled = false;
        return usageSnapshot;
    }

}
