// Copyright 2025 Rich Dougherty <rich@rd.nz>

package nz.rd.nonop.internal.logging;

/**
 * Small embeddable logging framework. We can delegate to others like SLF4J depending on runtime config.
 * <p>
 * Where possible we follow SLF4J naming conventions.
 */
public interface NonopLogger extends AutoCloseable {

    // TODO: Add other slf4j levels if needed, e.g. TRACE, FATAL
    enum Level {
        DEBUG,
        INFO,
        WARN,
        ERROR,
        OFF
    }

    Level getEnabledLevel();
    boolean isDebugEnabled();
    void debug(String message);
    void debug(String message, Throwable throwable);
    boolean isInfoEnabled();
    void info(String message);
    void info(String message, Throwable throwable);
    boolean isWarnEnabled();
    void warn(String message);
    void warn(String message, Throwable throwable);
    boolean isErrorEnabled();
    void error(String message);
    void error(String message, Throwable throwable);
}
