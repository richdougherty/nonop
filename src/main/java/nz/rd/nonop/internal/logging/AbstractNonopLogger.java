// Copyright 2025 Rich Dougherty <rich@rd.nz>

package nz.rd.nonop.internal.logging;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public abstract class AbstractNonopLogger implements NonopLogger {

    private final Level enabledLevel;

    public AbstractNonopLogger(Level enabledLevel) {
        this.enabledLevel = enabledLevel;
    }

    private void logIfEnabled(Level level, String message, Throwable throwable) {
        if (level.ordinal() < this.enabledLevel.ordinal()) {
            return;
        }
        outputLog(level, message, throwable);
    }

    /**
     * Subclasses should override this to output a log item which has been confirmed as needing to be logged.
     */
    protected abstract void outputLog(@NonNull Level level, @NonNull String message, @Nullable Throwable throwable);

    @Override
    public final Level getEnabledLevel() {
        return enabledLevel;
    }
    @Override
    public final boolean isDebugEnabled() {
        return enabledLevel.ordinal() <= Level.DEBUG.ordinal();
    }
    @Override
    public final void debug(String message) {
        logIfEnabled(Level.DEBUG, message, null);
    }
    @Override
    public final void debug(String message, Throwable throwable) {
        logIfEnabled(Level.DEBUG, message, throwable);

    }
    @Override
    public final boolean isInfoEnabled() {
        return enabledLevel.ordinal() <= Level.INFO.ordinal();
    }
    @Override
    public final void info(String message) {
        logIfEnabled(Level.INFO, message, null);
    }
    @Override
    public final void info(String message, Throwable throwable) {
        logIfEnabled(Level.INFO, message, throwable);

    }
    @Override
    public final boolean isWarnEnabled() {
        return enabledLevel.ordinal() <= Level.WARN.ordinal();
    }
    @Override
    public final void warn(String message) {
        logIfEnabled(Level.WARN, message, null);
    }

    @Override
    public final void warn(String message, Throwable throwable) {
        logIfEnabled(Level.WARN, message, throwable);
    }
    @Override
    public final boolean isErrorEnabled() {
        return enabledLevel.ordinal() <= Level.ERROR.ordinal();
    }
    @Override
    public final void error(String message) {
        logIfEnabled(Level.ERROR, message, null);
    }
    @Override
    public final void error(String message, Throwable throwable) {
        logIfEnabled(Level.ERROR, message, throwable);
    }
}
