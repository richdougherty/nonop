package nz.rd.nonop.config;

import nz.rd.nonop.internal.util.NonopLogger;

import java.util.Map;
import java.util.Objects;

public class AgentConfig {
    private final ScanConfig scanConfig;

    public AgentConfig(ScanConfig scanConfig) {
        this.scanConfig = scanConfig;
    }

    public static AgentConfig load(NonopLogger logger, Map<String, String> properties) {
        ScanConfig scanConfig = ScanConfig.load(logger, properties);
        return new AgentConfig(scanConfig);
    }

    public ScanConfig getScanConfig() {
        return scanConfig;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof AgentConfig)) return false;
        AgentConfig that = (AgentConfig) o;
        return Objects.equals(scanConfig, that.scanConfig);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(scanConfig);
    }

    @Override
    public String toString() {
        return "AgentConfig{" +
                "scanConfig=" + scanConfig +
                '}';
    }
}
