// Copyright 2025 Rich Dougherty <rich@rd.nz>

package nz.rd.nonop.internal.model;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class ClassUsageState {
    // TODO: Optimize memory by omitting this field if can always be provided by caller
    private final WeakReference<? extends Class<?>> clazzWeakRef;

    // Track method call states: no entry = unused, CALLED_ONCE = first call, CALLED_MULTIPLE = second+ call
    private final Map<String, MethodCallState> methodCallStates = new HashMap<>();
    private boolean reinstrumentationScheduled = false;

    public ClassUsageState(Class<?> clazz /*, Collection<String> methodSignatures */) {
        this.clazzWeakRef = new WeakReference<>(clazz);
    }

    public WeakReference<? extends Class<?>> getClazzWeakRef() {
        return clazzWeakRef;
    }

    private enum MethodCallState {
        CALLED_ONCE,
        CALLED_MULTIPLE
    }

    public enum MarkResult {
        FIRST_CALL_NO_ACTION(true, false),
        SECOND_CALL_INSTRUMENTATION_NEEDED(true, true),
        SECOND_CALL_INSTRUMENTATION_ALREADY_SCHEDULED(true, false),
        SUBSEQUENT_CALL_NO_ACTION(false, false);

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

    public synchronized MarkResult recordMethodUsedAndDecideIfInstrumentationNeeded(String methodSignature) {
        MethodCallState currentState = methodCallStates.get(methodSignature);

        if (currentState == null) {
            // First call - record it but don't trigger reinstrumentation yet
            methodCallStates.put(methodSignature, MethodCallState.CALLED_ONCE);
            return MarkResult.FIRST_CALL_NO_ACTION;
        } else if (currentState == MethodCallState.CALLED_ONCE) {
            // Second call - upgrade state and decide about reinstrumentation
            methodCallStates.put(methodSignature, MethodCallState.CALLED_MULTIPLE);

            if (reinstrumentationScheduled) {
                return MarkResult.SECOND_CALL_INSTRUMENTATION_ALREADY_SCHEDULED;
            } else {
                reinstrumentationScheduled = true;
                return MarkResult.SECOND_CALL_INSTRUMENTATION_NEEDED;
            }
        } else {
            // Third+ call - no action needed
            return MarkResult.SUBSEQUENT_CALL_NO_ACTION;
        }
    }

    public synchronized Set<String> recordInstrumentationWithSnapshotOfUsage() {
        // Create snapshot of all methods that have been called at least once
        Set<String> usageSnapshot = new HashSet<>(methodCallStates.keySet());
        reinstrumentationScheduled = false;
        return usageSnapshot;
    }
}