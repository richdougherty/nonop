// Copyright 2025 Rich Dougherty <rich@rd.nz>

package nz.rd.nonop.config;

import nz.rd.nonop.internal.util.NonopLogger;

import java.util.Map;
import java.util.Objects;

public class AgentConfig {
    private final ScanConfig scanConfig;
    private final OutputConfig outputConfig;

    public AgentConfig(ScanConfig scanConfig, OutputConfig outputConfig) {
        this.scanConfig = scanConfig;
        this.outputConfig = outputConfig;
    }

    public static AgentConfig load(NonopLogger logger, Map<String, String> properties) throws ConfigException {
        ScanConfig scanConfig = ScanConfig.load(logger, properties);
        OutputConfig outputConfig = OutputConfig.load(logger, properties);
        return new AgentConfig(scanConfig, outputConfig);
    }

    public ScanConfig getScanConfig() {
        return scanConfig;
    }

    public OutputConfig getOutputConfig() {
        return outputConfig;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof AgentConfig)) return false;
        AgentConfig that = (AgentConfig) o;
        return Objects.equals(scanConfig, that.scanConfig) && Objects.equals(outputConfig, that.outputConfig);
    }

    @Override
    public int hashCode() {
        return Objects.hash(scanConfig, outputConfig);
    }

    @Override
    public String toString() {
        return "AgentConfig{" +
                "scanConfig=" + scanConfig +
                ", outputConfig=" + outputConfig +
                '}';
    }
}
