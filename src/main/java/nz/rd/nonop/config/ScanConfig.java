// Copyright 2025 Rich Dougherty <rich@rd.nz>

package nz.rd.nonop.config;

import nz.rd.nonop.internal.config.ScanMatcher;
import nz.rd.nonop.internal.config.ScanRuleParser;
import nz.rd.nonop.internal.logging.NonopLogger;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public class ScanConfig {
    private final List<ScanMatcher> userScanMatchers;
    private final List<ScanMatcher> builtinScanMatchers;
    private final boolean scanIncludeBootstrap;
    private final boolean scanIncludeUnnamed;
    private final boolean scanIncludeSynthetic;

    public ScanConfig(List<ScanMatcher> builtinScanMatchers, List<ScanMatcher> userScanMatchers, boolean scanIncludeBootstrap, boolean scanIncludeUnnamed, boolean scanIncludeSynthetic) {
        this.userScanMatchers = userScanMatchers;
        this.builtinScanMatchers = builtinScanMatchers;
        this.scanIncludeBootstrap = scanIncludeBootstrap;
        this.scanIncludeUnnamed = scanIncludeUnnamed;
        this.scanIncludeSynthetic = scanIncludeSynthetic;
    }

    public static ScanConfig load(NonopLogger logger, Map<String, String> properties) throws ConfigException {
        ScanRuleParser parser = new ScanRuleParser(logger);

        String userRulesStr = properties.get("nonop.scan");
        List<ScanMatcher> parsedUserScanMatchers = parser.parse(userRulesStr);
        String builtinRulesStr = properties.get("nonop.scan.builtin");
        List<ScanMatcher> parsedBuiltinScanMatchers = parser.parse(builtinRulesStr);

        boolean includeBootstrap = Boolean.parseBoolean(properties.get("nonop.scan.include.bootstrap"));
        boolean includeUnnamed = Boolean.parseBoolean(properties.get("nonop.scan.include.unnamed"));
        boolean includeSynthetic = Boolean.parseBoolean(properties.get("nonop.scan.include.synthetic"));

        return new ScanConfig(parsedBuiltinScanMatchers, parsedUserScanMatchers, includeBootstrap, includeUnnamed, includeSynthetic);
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
        if (!(o instanceof ScanConfig)) return false;
        ScanConfig that = (ScanConfig) o;
        return scanIncludeBootstrap == that.scanIncludeBootstrap && scanIncludeUnnamed == that.scanIncludeUnnamed && scanIncludeSynthetic == that.scanIncludeSynthetic && Objects.equals(userScanMatchers, that.userScanMatchers) && Objects.equals(builtinScanMatchers, that.builtinScanMatchers);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userScanMatchers, builtinScanMatchers, scanIncludeBootstrap, scanIncludeUnnamed, scanIncludeSynthetic);
    }
}
