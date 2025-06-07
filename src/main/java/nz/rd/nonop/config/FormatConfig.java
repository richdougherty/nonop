// Copyright 2025 Rich Dougherty <rich@rd.nz>

package nz.rd.nonop.config;

import nz.rd.nonop.internal.logging.NonopLogger;

import java.util.Map;
import java.util.Objects;

/**
 * Configuration for the output format.
 */
public final class FormatConfig {

    public enum FormatType {
        /** Human-readable, single-line format. */
        SIMPLE,
        /** Machine-readable JSON format. */
        JSON
    }

    private static final FormatType DEFAULT_FORMAT_TYPE = FormatType.SIMPLE;

    private final FormatType formatType;

    private FormatConfig(FormatType formatType) {
        this.formatType = Objects.requireNonNull(formatType, "formatType must not be null");
    }

    public static FormatConfig load(NonopLogger logger, Map<String, String> properties) throws ConfigException {
        String formatStr = properties.get("nonop.format");
        if (formatStr == null) {
            throw new ConfigException("Missing required property: nonop.format");
        }
        formatStr = formatStr.trim();
        if (formatStr.isEmpty()) {
            throw new ConfigException("nonop.format cannot be empty. Specify 'json' or 'simple'.");
        }

        FormatType formatType;
        try {
            formatType = FormatType.valueOf(formatStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ConfigException("Invalid value for nonop.format: '" + formatStr + "'. Must be 'simple' or 'json'.");
        }
        return new FormatConfig(formatType);
    }

    public FormatType getFormatType() {
        return formatType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FormatConfig that = (FormatConfig) o;
        return formatType == that.formatType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(formatType);
    }

    @Override
    public String toString() {
        return "FormatConfig{" +
                "formatType=" + formatType +
                '}';
    }
}