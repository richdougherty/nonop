// Copyright 2025 Rich Dougherty <rich@rd.nz>

package nz.rd.nonop.internal;

import nz.rd.nonop.internal.model.ClassLoaderRegistry;
import nz.rd.nonop.internal.model.ClassUsageState;
import nz.rd.nonop.internal.model.JVMRegistry;
import nz.rd.nonop.internal.reporting.UsageReporter;
import nz.rd.nonop.internal.util.NonopLogger;

import java.lang.instrument.Instrumentation;
import java.util.Set;

public final class NonopCore implements NonopStaticHooks.MethodCalled, NonopClassfileTransformer.GetMethodUsageSnapshot {

    private final NonopLogger nonopLogger;
    private final Instrumentation instrumentation;
    private final UsageReporter usageReporter;

    private final JVMRegistry jvmRegistry = new JVMRegistry();

    // Private constructor to prevent instantiation
    public NonopCore(NonopLogger nonopLogger, Instrumentation inst, UsageReporter usageReporter) {
        this.nonopLogger = nonopLogger;
        this.instrumentation = inst;
        this.usageReporter = usageReporter;
    }

    public ClassUsageState getClassUsageState(Class<?> clazz) {
        ClassLoader classLoader = clazz.getClassLoader();
        ClassLoaderRegistry classLoaderRegistry = jvmRegistry.getClassLoaderRegistry(classLoader);
        return classLoaderRegistry.getOrCreateClassUsageState(clazz);
    }

    // Called by instrumented code (Phase 0)
    public void methodCalled(Class<?> clazz, String methodSignature) {
        try {
            nonopLogger.debug("methodCalled hook invoked: " + clazz.getCanonicalName() + " " + methodSignature);
            ClassUsageState classUsageState = getClassUsageState(clazz);
            ClassUsageState.MarkResult markResult = classUsageState.recordMethodUsedAndDecideIfInstrumentationNeeded(methodSignature);

            if (markResult.isInstrumentationNeeded()) {
                scheduleRetransformation(classUsageState);
            }

            if (markResult.isAdded()) {
                // Retain strong reference until reported
                usageReporter.recordMethodFirstUsage(clazz, methodSignature);
            }
        } catch (Exception e) {
            nonopLogger.error("Error in methodCalled", e);
        }
    }

    public interface ScheduleRetransformation {
        void scheduleRetransformation(ClassUsageState classUsageState);
    }

    private void scheduleRetransformation(ClassUsageState classUsageState) {
        nonopLogger.debug("scheduleRetransformation: " + classUsageState.getClazzWeakRef().get());
        // TODO: Schedule and run in a different thread, async, etc
        Class<?> clazz = classUsageState.getClazzWeakRef().get();
        if (clazz == null) {
            // Class reference lost (unloaded) since retransformation scheduled; can skip retransformation since
            // class cannot be called again now.
            // The actual usage of the methods by this class will still have been recorded elsewhere, we just don't
            // need to do the optimisation of reinstrumenting the class now that it has been unloaded.
            return;
        }

        try {
            instrumentation.retransformClasses(clazz);
        } catch (Exception e) {
            nonopLogger.error("Failed to reinstrument classes to remove method usage instrumentation of already used methods; performance may suffer.", e);
        }
        // TODO: Schedule and run in a different thread
    }

    @Override
    public Set<String> usageSnapshotForInstrumentation(Class<?> clazz) {
        return getClassUsageState(clazz).recordInstrumentationWithSnapshotOfUsage();
    }
}