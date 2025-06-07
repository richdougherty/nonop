// Copyright 2025 Rich Dougherty <rich@rd.nz>

package nz.rd.nonop.config;

import nz.rd.nonop.internal.logging.NonopLogger;

import java.util.Map;
import java.util.Objects;

public class AgentConfig {
    private final ScanConfig scanConfig;
    private final OutputConfig outputConfig;
    private final FormatConfig formatConfig;
    private final LogConfig logConfig;

    public AgentConfig(ScanConfig scanConfig, OutputConfig outputConfig, FormatConfig formatConfig, LogConfig logConfig) {
        this.scanConfig = scanConfig;
        this.outputConfig = outputConfig;
        this.formatConfig = formatConfig;
        this.logConfig = logConfig;
    }

    public static AgentConfig load(NonopLogger logger, Map<String, String> properties) throws ConfigException {
        ScanConfig scanConfig = ScanConfig.load(logger, properties);
        OutputConfig outputConfig = OutputConfig.load(logger, properties);
        FormatConfig formatConfig = FormatConfig.load(logger, properties);
        LogConfig logConfig = LogConfig.load(logger, properties);
        return new AgentConfig(scanConfig, outputConfig, formatConfig, logConfig);
    }

    public ScanConfig getScanConfig() {
        return scanConfig;
    }

    public OutputConfig getOutputConfig() {
        return outputConfig;
    }

    public FormatConfig getFormatConfig() {
        return formatConfig;
    }

    public LogConfig getLogConfig() {
        return logConfig;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof AgentConfig)) return false;
        AgentConfig that = (AgentConfig) o;
        return Objects.equals(scanConfig, that.scanConfig) &&
                Objects.equals(outputConfig, that.outputConfig) &&
                Objects.equals(formatConfig, that.formatConfig) &&
                Objects.equals(logConfig, that.logConfig);
    }

    @Override
    public int hashCode() {
        return Objects.hash(scanConfig, outputConfig, formatConfig, logConfig);
    }

    @Override
    public String toString() {
        return "AgentConfig{" +
                "scanConfig=" + scanConfig +
                ", outputConfig=" + outputConfig +
                ", formatConfig=" + formatConfig +
                ", logConfig=" + logConfig +
                '}';
    }
}