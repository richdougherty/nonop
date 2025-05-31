// Copyright 2025 Rich Dougherty <rich@rd.nz>

package nz.rd.nonop;

import nz.rd.nonop.internal.NonopClassfileTransformer;
import nz.rd.nonop.internal.NonopCore;
import nz.rd.nonop.internal.NonopStaticHooks;
import nz.rd.nonop.internal.reporting.LoggingUsageReporter;
import nz.rd.nonop.internal.util.NonopConsoleLogger;
import nz.rd.nonop.internal.util.NonopLogger;

import java.lang.instrument.Instrumentation;

public class NonopAgent implements AutoCloseable {

    private final LoggingUsageReporter usageReporting;
    private final NonopLogger nonopLogger;

    public static void premain(String agentArgs, Instrumentation instrumentation) {
        NonopLogger nonopLogger = new NonopConsoleLogger(false);
        nonopLogger.debug("[nonop] Initializing Nonop agent with instrumentation: " + instrumentation +
                ", args: " + (agentArgs == null ? "<none>" : agentArgs));

        NonopAgent agent = new NonopAgent(nonopLogger, instrumentation);
        Runtime.getRuntime().addShutdownHook(new Thread(agent::close));

    }

    public NonopAgent(NonopLogger nonopLogger, Instrumentation instrumentation) {
        this.nonopLogger = nonopLogger;
        usageReporting = new LoggingUsageReporter(nonopLogger);
        NonopCore core = new NonopCore(nonopLogger, instrumentation, usageReporting);

        NonopStaticHooks.initialize(core);
        NonopClassfileTransformer transformer = new NonopClassfileTransformer(core, nonopLogger);
        instrumentation.addTransformer(transformer, true); // true for canRetransform
        nonopLogger.debug("Agent initialized and transformer added.");

    }

    @Override
    public void close() {
        nonopLogger.debug("[nonop] Closing Nonop agent and reporting usage on shutdown.");
        usageReporting.finishUsageReportingOnShutdown();
        // TODO: Close other resources, e.g. threads
        // TODO: Consider whether to have an optimized close for shutting down faster, i.e. only flush the report, don't worry about other resources
    }
}