package nz.rd.nonop.config;

import nz.rd.nonop.config.scan.ScanMatcher;
import nz.rd.nonop.config.scan.ScanRuleParser;
import nz.rd.nonop.internal.util.NonopLogger;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public class AgentConfig {
    private final List<ScanMatcher> userScanMatchers;
    private final List<ScanMatcher> builtinScanMatchers;
    private final boolean scanIncludeBootstrap;
    private final boolean scanIncludeUnnamed;
    private final boolean scanIncludeSynthetic;

    public AgentConfig(List<ScanMatcher> builtinScanMatchers, List<ScanMatcher> userScanMatchers, boolean scanIncludeBootstrap, boolean scanIncludeUnnamed, boolean scanIncludeSynthetic) {
        this.userScanMatchers = userScanMatchers;
        this.builtinScanMatchers = builtinScanMatchers;
        this.scanIncludeBootstrap = scanIncludeBootstrap;
        this.scanIncludeUnnamed = scanIncludeUnnamed;
        this.scanIncludeSynthetic = scanIncludeSynthetic;
    }

    public static AgentConfig load(NonopLogger logger, Map<String, String> properties) {
        ScanRuleParser parser = new ScanRuleParser(logger);

        String userRulesStr = properties.get("nonop.scan");
        List<ScanMatcher> parsedUserScanMatchers = parser.parse(userRulesStr);
        String builtinRulesStr = properties.get("nonop.scan.builtin");
        List<ScanMatcher> parsedBuiltinScanMatchers = parser.parse(builtinRulesStr);

        boolean includeBootstrap = Boolean.parseBoolean(properties.get("nonop.scan.include.bootstrap"));
        boolean includeUnnamed = Boolean.parseBoolean(properties.get("nonop.scan.include.unnamed"));
        boolean includeSynthetic = Boolean.parseBoolean(properties.get("nonop.scan.include.synthetic"));


        AgentConfig config = new AgentConfig(parsedBuiltinScanMatchers, parsedUserScanMatchers, includeBootstrap, includeUnnamed, includeSynthetic);
        logger.debug("[nonop-config] Loaded config: " + config);
        return config;
    }

    public List<ScanMatcher> getUserScanMatchers() {
        return userScanMatchers;
    }

    public List<ScanMatcher> getBuiltinScanMatchers() {
        return builtinScanMatchers;
    }

    public boolean isScanIncludeBootstrap() {
        return scanIncludeBootstrap;
    }

    public boolean isScanIncludeUnnamed() {
        return scanIncludeUnnamed;
    }

    public boolean isScanIncludeSynthetic() {
        return scanIncludeSynthetic;
    }

    @Override
    public String toString() {
        return "AgentConfig{" + "userScanMatchers=" + userScanMatchers + ", builtinScanMatchers=" + builtinScanMatchers + ", scanIncludeBootstrap=" + scanIncludeBootstrap + ", scanIncludeUnnamed=" + scanIncludeUnnamed + ", scanIncludeSynthetic=" + scanIncludeSynthetic + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof AgentConfig)) return false;
        AgentConfig that = (AgentConfig) o;
        return scanIncludeBootstrap == that.scanIncludeBootstrap && scanIncludeUnnamed == that.scanIncludeUnnamed && scanIncludeSynthetic == that.scanIncludeSynthetic && Objects.equals(userScanMatchers, that.userScanMatchers) && Objects.equals(builtinScanMatchers, that.builtinScanMatchers);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userScanMatchers, builtinScanMatchers, scanIncludeBootstrap, scanIncludeUnnamed, scanIncludeSynthetic);
    }
}
