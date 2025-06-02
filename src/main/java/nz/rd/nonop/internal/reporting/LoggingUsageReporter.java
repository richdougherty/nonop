// Copyright 2025 Rich Dougherty <rich@rd.nz>

package nz.rd.nonop.internal.reporting;

import nz.rd.nonop.internal.reporting.format.UsageEventFormatter;
import nz.rd.nonop.internal.util.NonopLogger;

// TODO: Make optimised version to run in a different thread, async etc
public final class LoggingUsageReporter implements UsageReporter {

    private final NonopLogger nonopLogger;
    private final UsageEventFormatter formatter;

    public LoggingUsageReporter(NonopLogger nonopLogger, UsageEventFormatter formatter) {
        this.nonopLogger = nonopLogger;
        this.formatter = formatter;
    }

    public void recordMethodFirstUsage(long callTimestampMillis, Class<?> clazz, String methodName, String methodDescriptor) {
        nonopLogger.info(formatter.formatMethodCalled(
                callTimestampMillis,
                clazz.getCanonicalName(),
                methodName,
                methodDescriptor
        ));
    }

    @Override
    public void finishUsageReportingOnShutdown() {
        // TODO: Add implementation
    }
}
