// Copyright 2025 Rich Dougherty <rich@rd.nz>

package nz.rd.nonop.internal.reporting;

public interface UsageReporter {
    void recordMethodFirstUsage(Class<?> clazz, String methodSignature);
    void finishUsageReportingOnShutdown();
}
