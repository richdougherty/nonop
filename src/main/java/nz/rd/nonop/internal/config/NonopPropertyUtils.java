// Copyright 2025 Rich Dougherty <rich@rd.nz>

package nz.rd.nonop.internal.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Helper for working with properties.
 */
public class NonopPropertyUtils {

    public static Map<String, String> loadNonopSystemPropertiesWithDefaults() throws IOException {
        Map<String, String> nonopDefaults = loadNonopDefaults();
        Map<String, String> nonopSystemProps = toNonopPropertiesMap(System.getProperties());

        Map<String, String> combined = new HashMap<>();
        combined.putAll(nonopDefaults);
        combined.putAll(nonopSystemProps); // Overwrites defaults if set
        return combined;
    }

    public static Map<String, String> loadNonopDefaults() throws IOException {
        Properties defaultProperties = new Properties();
        try (InputStream resourceAsStream = NonopPropertyUtils.class.getResourceAsStream("/nz/rd/nonop/config/nonop-default.properties")) {
            defaultProperties.load(resourceAsStream);
        }
        return toNonopPropertiesMap(defaultProperties);
    }

    private static Map<String, String> toNonopPropertiesMap(Properties properties) throws IOException {
        Map<String, String> stringMap = new HashMap<>();
        for (Map.Entry<Object, Object> entry : properties.entrySet()) {
            // Ensure we don't override any existing system properties
            if (entry.getKey().toString().startsWith("nonop.")) {
                stringMap.put(entry.getKey().toString(), entry.getValue().toString());
            }
        }
        return stringMap;
    }
}
