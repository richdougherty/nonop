// Copyright 2025 Rich Dougherty <rich@rd.nz>

package nz.rd.nonop.internal.reporting;

public interface UsageReporter {
    // TODO: Consider using class name as a String
    void recordMethodFirstUsage(long timestampMillis, Class<?> clazz, String methodName, String methodDescriptor);
    void finishUsageReportingOnShutdown();
}
