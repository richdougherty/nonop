// Copyright 2025 Rich Dougherty <rich@rd.nz>

package nz.rd.nonop;

import nz.rd.nonop.internal.NonopClassfileTransformer;
import nz.rd.nonop.internal.NonopCore;
import nz.rd.nonop.internal.NonopStaticHooks;
import nz.rd.nonop.internal.reporting.LoggingUsageReporter;
import nz.rd.nonop.internal.reporting.UsageReporter;
import nz.rd.nonop.internal.reporting.format.JsonUsageEventFormatter;
import nz.rd.nonop.internal.util.NonopConsoleLogger;
import nz.rd.nonop.internal.util.NonopLogger;

import java.lang.instrument.Instrumentation;

public class NonopAgent implements AutoCloseable {

    private final UsageReporter usageReporter;
    private final NonopLogger nonopLogger;

    public static void premain(String agentArgs, Instrumentation instrumentation) {
        // TODO: Consider adding protection from IntelliJ's double-run agent bug under Gradle by making premain calls with identical args idempotent?
        // https://youtrack.jetbrains.com/issue/IDEA-235974/Java-instrumentation-premain-gets-called-twice-with-Gradle-run-delegation

        // TODO: Command line argument to control logging and debug level

        NonopLogger nonopLogger = new NonopConsoleLogger(true);
        nonopLogger.debug("[nonop] Initializing Nonop agent with instrumentation: " + instrumentation +
                ", args: " + (agentArgs == null ? "<none>" : agentArgs));

        NonopAgent agent = new NonopAgent(nonopLogger, instrumentation);
        Runtime.getRuntime().addShutdownHook(new Thread(agent::close));

    }

    public NonopAgent(NonopLogger nonopLogger, Instrumentation instrumentation) {
        this.nonopLogger = nonopLogger;
        JsonUsageEventFormatter jsonUsageEventFormatter = new JsonUsageEventFormatter();
        usageReporter = new LoggingUsageReporter(nonopLogger, jsonUsageEventFormatter);
        NonopCore core = new NonopCore(nonopLogger, instrumentation, usageReporter);

        NonopStaticHooks.initialize(core);
        NonopClassfileTransformer transformer = new NonopClassfileTransformer(core, nonopLogger);
        instrumentation.addTransformer(transformer, true); // true for canRetransform
        nonopLogger.debug("Agent initialized and transformer added.");

    }

    @Override
    public void close() {
        nonopLogger.debug("[nonop] Closing Nonop agent and reporting usage on shutdown.");
        usageReporter.finishUsageReportingOnShutdown();
        // TODO: Close other resources, e.g. threads
        // TODO: Consider whether to have an optimized close for shutting down faster, i.e. only flush the report, don't worry about other resources
    }
}