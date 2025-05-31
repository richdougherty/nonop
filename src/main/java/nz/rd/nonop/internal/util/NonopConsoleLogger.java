// Copyright 2025 Rich Dougherty <rich@rd.nz>

package nz.rd.nonop.internal.util;

public final class NonopConsoleLogger implements NonopLogger {

    private final boolean debugEnabled;

    public NonopConsoleLogger(boolean debugEnabled) {
        this.debugEnabled = debugEnabled;
    }

    @Override
    public boolean isDebugEnabled() {
        return debugEnabled;
    }

    @Override
    public void debug(String message) {
        if (debugEnabled) {
            System.out.println("[nonop] DEBUG " + message);
        }
    }

    @Override
    public void debug(String message, Throwable throwable) {
        debug(message);
        if (throwable != null) {
            throwable.printStackTrace(System.out);
        }
    }

    @Override
    public boolean isInfoEnabled() {
        return true;
    }

    @Override
    public void info(String message) {
        System.out.println("[nonop] INFO  " + message);
    }

    @Override
    public void info(String message, Throwable throwable) {
        info(message);
        if (throwable != null) {
            throwable.printStackTrace(System.out);
        }
    }

    @Override
    public boolean isWarnEnabled() {
        return true;
    }

    @Override
    public void warn(String message) {
        System.out.println("[nonop] WARN  " + message);
    }

    @Override
    public void warn(String message, Throwable throwable) {
        warn(message);
        if (throwable != null) {
            throwable.printStackTrace(System.out);
        }
    }

    @Override
    public boolean isErrorEnabled() {
        return true;
    }

    @Override
    public void error(String message) {
        System.out.println("[nonop] ERROR " + message);
    }

    @Override
    public void error(String message, Throwable throwable) {
        error(message);
        if (throwable != null) {
            throwable.printStackTrace(System.out);
        }
    }
}
