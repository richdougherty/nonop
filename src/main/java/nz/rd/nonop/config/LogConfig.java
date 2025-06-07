// Copyright 2025 Rich Dougherty <rich@rd.nz>

package nz.rd.nonop.config;

import nz.rd.nonop.internal.logging.NonopLogger;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;

/**
 * Configuration for logging.
 */
public final class LogConfig {

    private final NonopLogger.Level level;

    private LogConfig(NonopLogger.Level level) {
        this.level = Objects.requireNonNull(level, "level must not be null");
    }

    public static LogConfig load(NonopLogger logger, Map<String, String> properties) throws ConfigException {
        String levelStr = properties.get("nonop.log.level");
        if (levelStr == null) {
            throw new ConfigException("Missing required property: nonop.log.level");
        }
        levelStr = levelStr.trim();
        if (levelStr.isEmpty()) {
            throw new ConfigException("nonop.log.level cannot be empty.");
        }

        NonopLogger.Level logLevel;
        try {
            logLevel = NonopLogger.Level.valueOf(levelStr.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ConfigException("Invalid value for nonop.log.level: '" + levelStr +
                    "'. Must be one of: " + Arrays.toString(NonopLogger.Level.values()));
        }

        return new LogConfig(logLevel);
    }

    // TODO: Consider if we should abstract this as NonopLogger.Level is technically an internal type
    public NonopLogger.Level getLevel() {
        return level;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LogConfig logConfig = (LogConfig) o;
        return level == logConfig.level;
    }

    @Override
    public int hashCode() {
        return Objects.hash(level);
    }

    @Override
    public String toString() {
        return "LogConfig{" +
                "level=" + level +
                '}';
    }
}