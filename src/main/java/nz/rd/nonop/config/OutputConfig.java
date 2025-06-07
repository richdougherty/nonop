// Copyright 2025 Rich Dougherty <rich@rd.nz>

package nz.rd.nonop.config;

import nz.rd.nonop.internal.logging.NonopLogger;

import java.util.Map;
import java.util.Objects;

/**
 * Configuration for nonop's output destination and buffering.
 * This class is immutable.
 */
public final class OutputConfig {

    // Default buffer size of 2MB, as specified in the prompt.
    private static final int DEFAULT_BUFFER_SIZE = 2 * 1024 * 1024; // 2097152

    // --- Start of ADT for OutputTarget ---

    /**
     * A sealed-interface-style ADT representing the output destination.
     * Can be a {@link StandardStream} or a {@link File}.
     */
    public interface OutputTarget {
    }

    /**
     * Represents output to standard out or standard error.
     */
    public static final class StandardStream implements OutputTarget {
        public enum Type { STDOUT, STDERR }

        private final Type type;

        public StandardStream(Type type) {
            this.type = Objects.requireNonNull(type, "type must not be null");
        }

        public Type getType() {
            return type;
        }

        @Override
        public String toString() {
            return "StandardStream{type=" + type + '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            StandardStream that = (StandardStream) o;
            return type == that.type;
        }

        @Override
        public int hashCode() {
            return Objects.hash(type);
        }
    }

    /**
     * Represents output to a file path.
     */
    public static final class File implements OutputTarget {
        private final String path;

        public File(String path) {
            this.path = Objects.requireNonNull(path, "path must not be null");
        }

        public String getPath() {
            return path;
        }

        @Override
        public String toString() {
            return "File{path='" + path + "'}";
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            File file = (File) o;
            return path.equals(file.path);
        }

        @Override
        public int hashCode() {
            return Objects.hash(path);
        }
    }

    // --- End of ADT ---


    private final OutputTarget outputTarget;
    private final int bufferSize;

    private OutputConfig(OutputTarget outputTarget, int bufferSize) {
        this.outputTarget = outputTarget;
        this.bufferSize = bufferSize;
    }

    /**
     * Loads the output configuration from a properties map.
     *
     * @param logger     The logger for reporting warnings.
     * @param properties The map of properties (e.g., from system properties).
     * @return A new, configured {@link OutputConfig} instance.
     */
    public static OutputConfig load(NonopLogger logger, Map<String, String> properties) throws ConfigException {
        // 1. Parse the output target (nonop.out)
        String outStr = properties.get("nonop.out");
        if (outStr == null) {
            throw new ConfigException("Missing required property: nonop.out");
        }
        outStr = outStr.trim();
        if (outStr.isEmpty()) {
            throw new ConfigException("nonop.out cannot be empty. Specify 'stdout', 'stderr', or a file path.");
        }

        OutputTarget target;
        if ("stdout".equalsIgnoreCase(outStr)) { // FIXME: Consider locale for case
            target = new StandardStream(StandardStream.Type.STDOUT); // Default to stdout
        } else if ("stderr".equalsIgnoreCase(outStr)) { // FIXME: Consider locale for case
            target = new StandardStream(StandardStream.Type.STDERR);
        } else {
            target = new File(outStr);
        }

        // 2. Parse the buffer size (nonop.out.buffersize)
        String bufferSizeStr = properties.get("nonop.out.buffersize");
        if (bufferSizeStr == null) {
            throw new ConfigException("Missing required property: nonop.out.buffersize");
        }
        bufferSizeStr = bufferSizeStr.trim();

        int bufferSize;
        try {
            bufferSize = Integer.parseInt(bufferSizeStr.trim());
            if (bufferSize < 0) {
                throw new ConfigException("Invalid value for nonop.out.buffersize: '" + bufferSizeStr + "'. Must be non-negative.");
            }
        } catch (NumberFormatException e) {
            throw new ConfigException("Invalid number format for nonop.out.buffersize: '" + bufferSizeStr + "'");
        }

        return new OutputConfig(target, bufferSize);
    }

    public OutputTarget getOutputTarget() {
        return outputTarget;
    }

    public int getBufferSize() {
        return bufferSize;
    }

    @Override
    public String toString() {
        return "OutputConfig{" +
                "outputTarget=" + outputTarget +
                ", bufferSize=" + bufferSize +
                '}';
    }
}