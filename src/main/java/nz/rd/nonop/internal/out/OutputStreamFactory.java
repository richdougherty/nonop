// Copyright 2025 Rich Dougherty <rich@rd.nz>

package nz.rd.nonop.internal.out;

import nz.rd.nonop.config.OutputConfig;
import nz.rd.nonop.internal.util.NonopLogger;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * A factory for creating an {@link OutputStream} based on an {@link OutputConfig}.
 * This class is not meant to be instantiated.
 */
public final class OutputStreamFactory {

    private OutputStreamFactory() {
        // Prevent instantiation
    }

    /**
     * Creates and returns an appropriate {@link OutputStream} based on the provided configuration.
     * The stream will be buffered if the configured buffer size is greater than zero.
     *
     * @param config The output configuration.
     * @param logger The logger for reporting errors during file creation.
     * @return A configured OutputStream.
     * @throws IOException If a file-based output stream cannot be created.
     */
    public static OutputStream create(OutputConfig config, NonopLogger logger) throws IOException {
        final OutputConfig.OutputTarget target = config.getOutputTarget();
        final OutputStream rawStream;

        // Use 'instanceof' to switch on the ADT type
        if (target instanceof OutputConfig.StandardStream) {
            OutputConfig.StandardStream standardStream = (OutputConfig.StandardStream) target;
            if (standardStream.getType() == OutputConfig.StandardStream.Type.STDOUT) {
                rawStream = System.out;
            } else {
                rawStream = System.err;
            }
        } else if (target instanceof OutputConfig.File) {
            OutputConfig.File fileTarget = (OutputConfig.File) target;
            String path = fileTarget.getPath();
            logger.info("Configuring output to file: " + path);
            try {
                rawStream = new FileOutputStream(path);
            } catch (IOException e) {
                logger.error("Failed to open output file '"+ path + "'.", e);
                throw e; // Re-throw to halt agent initialization
            }
        } else {
            // This should be unreachable if OutputConfig is well-defined.
            // It's a safeguard against future programming errors.
            throw new IllegalStateException("Unknown OutputTarget type: " + target.getClass().getName());
        }

        // Apply buffering if configured
        int bufferSize = config.getBufferSize();
        if (bufferSize > 0) {
            return new BufferedOutputStream(rawStream, bufferSize);
        } else {
            return rawStream; // Return unbuffered stream if size is 0 or less
        }
    }
}