// Copyright 2025 Rich Dougherty <rich@rd.nz>

package nz.rd.nonop;

import nz.rd.nonop.config.AgentConfig;
import nz.rd.nonop.internal.NonopCore;
import nz.rd.nonop.internal.NonopStaticHooks;
import nz.rd.nonop.internal.config.NonopPropertyUtils;
import nz.rd.nonop.internal.reporting.OutputUsageReporter;
import nz.rd.nonop.internal.reporting.UsageReporter;
import nz.rd.nonop.internal.reporting.format.UsageEventFormatter;
import nz.rd.nonop.internal.transformer.NonopClassfileTransformer;
import nz.rd.nonop.internal.util.NonopConsoleLogger;
import nz.rd.nonop.internal.util.NonopLogger;

import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.util.Map;

public class NonopAgent implements AutoCloseable {

    private final UsageReporter usageReporter;
    private final NonopLogger nonopLogger;

    public static void premain(String agentArgs, Instrumentation instrumentation) throws Exception {
        // TODO: Consider adding protection from IntelliJ's double-run agent bug under Gradle by making premain calls with identical args idempotent?
        // https://youtrack.jetbrains.com/issue/IDEA-235974/Java-instrumentation-premain-gets-called-twice-with-Gradle-run-delegation

        // TODO: Command line argument to control logging and debug level

        NonopLogger nonopLogger = new NonopConsoleLogger(true);
        nonopLogger.debug("[nonop] Initializing Nonop agent with instrumentation: " + instrumentation +
                ", args: " + (agentArgs == null ? "<none>" : agentArgs));

        Map<String, String> properties = NonopPropertyUtils.loadNonopSystemPropertiesWithDefaults();
        nonopLogger.debug("[nonop] Loaded nonop properties (system props combined with defaults): " + properties);

        AgentConfig agentConfig = AgentConfig.load(nonopLogger, properties);
        nonopLogger.debug("[nonop] Agent configuration loaded: " + agentConfig);

        @SuppressWarnings("resource") // Closed in shutdown hook
        NonopAgent agent = new NonopAgent(nonopLogger, agentConfig, instrumentation);
        Runtime.getRuntime().addShutdownHook(new Thread(agent::close));
    }

    public NonopAgent(NonopLogger nonopLogger, AgentConfig agentConfig, Instrumentation instrumentation) throws IOException {
        this.nonopLogger = nonopLogger;

        UsageEventFormatter usageEventFormatter = UsageEventFormatter.createFromConfig(agentConfig.getFormatConfig());
        usageReporter = new OutputUsageReporter(nonopLogger, agentConfig.getOutputConfig(), usageEventFormatter);
        NonopCore core = new NonopCore(nonopLogger, instrumentation, usageReporter);

        NonopClassfileTransformer transformer = new NonopClassfileTransformer(agentConfig.getScanConfig(), core, nonopLogger);

        NonopStaticHooks.initialize(core);
        instrumentation.addTransformer(transformer, true); // true for canRetransform
        nonopLogger.debug("Agent initialized and transformer added.");

    }

    @Override
    public void close() {
        nonopLogger.debug("Closing agent and reporting usage on shutdown.");

        try {
            usageReporter.finishUsageReportingOnShutdown();
        } catch (Exception e) {
            nonopLogger.error("Error occurred shutting down usage reporter.", e);
            // Continue shutdown
        }
        // TODO: Close other resources, e.g. threads
        // TODO: Consider whether to have an optimized close for shutting down faster, i.e. only flush the report, don't worry about other resources
    }
}