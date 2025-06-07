// Copyright 2025 Rich Dougherty <rich@rd.nz>

package nz.rd.nonop.internal.reporting;

import nz.rd.nonop.config.OutputConfig;
import nz.rd.nonop.internal.logging.NonopLogger;
import nz.rd.nonop.internal.out.OutputStreamFactory;
import nz.rd.nonop.internal.reporting.format.UsageEventFormatter;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;

// TODO: Make optimised version to run in a different thread, async etc
public final class OutputUsageReporter implements UsageReporter {

    private final NonopLogger nonopLogger;
    private final UsageEventFormatter formatter;
    private final PrintWriter writer;

    public OutputUsageReporter(NonopLogger nonopLogger, OutputConfig outputConfig, UsageEventFormatter formatter) throws IOException {
        this.nonopLogger = nonopLogger;
        this.formatter = formatter;

        OutputStream configuredOutputStream = OutputStreamFactory.create(outputConfig, nonopLogger);
        OutputStream bufferedOutputStream = new BufferedOutputStream(configuredOutputStream, outputConfig.getBufferSize());
        this.writer = new PrintWriter(bufferedOutputStream, true); // Auto-flush on newline // TODO: Consider buffering/flushing
    }

    public void recordMethodFirstUsage(long callTimestampMillis, Class<?> clazz, String methodName, String methodDescriptor) {
        this.writer.println(formatter.formatMethodCalled(
                callTimestampMillis,
                clazz.getCanonicalName(),
                methodName,
                methodDescriptor
        ));
    }

    @Override
    public void finishUsageReportingOnShutdown() throws IOException {
        writer.flush();
        writer.close();
    }
}
