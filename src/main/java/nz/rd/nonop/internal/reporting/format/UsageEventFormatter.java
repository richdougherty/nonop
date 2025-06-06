// Copyright 2025 Rich Dougherty <rich@rd.nz>

package nz.rd.nonop.internal.reporting.format;

import nz.rd.nonop.config.FormatConfig;

// TODO: Consider a close() method in case formatters hold resources
public interface UsageEventFormatter {

    String formatMethodCalled(long callTimestampMillis, String className, String methodName, String methodDescriptor);

    /**
     * Creates a {@link UsageEventFormatter} instance based on the provided configuration.
     *
     * @param config The format configuration.
     * @return A new formatter instance.
     */
    static UsageEventFormatter createFromConfig(FormatConfig config) {
        switch (config.getFormatType()) {
            case SIMPLE:
                return new SimpleUsageEventFormatter();
            case JSON:
                return new JsonUsageEventFormatter();
            default:
                // This case should be unreachable if the enum is exhaustive
                throw new IllegalStateException("Unsupported format type: " + config.getFormatType());
        }
    }
}