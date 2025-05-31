// Copyright 2025 Rich Dougherty <rich@rd.nz>

package nz.rd.nonop.internal.reporting;

import nz.rd.nonop.internal.util.NonopLogger;

// TODO: Make optimised version to run in a different thread, async etc
public final class LoggingUsageReporter implements UsageReporting {

    private final NonopLogger nonopLogger;

    public LoggingUsageReporter(NonopLogger nonopLogger) {
        this.nonopLogger = nonopLogger;
    }

    public void recordMethodFirstUsage(Class<?> clazz, String methodSignature) {
        nonopLogger.info("METHOD_CALLED: " + clazz.getCanonicalName() + " " + methodSignature);
    }

    @Override
    public void finishUsageReportingOnShutdown() {
        // TODO: Add implementation
    }
}
