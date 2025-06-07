// Copyright 2025 Rich Dougherty <rich@rd.nz>

package nz.rd.nonop.internal.logging;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class ConsoleNonopLogger extends AbstractNonopLogger {

    public ConsoleNonopLogger(Level level) {
        super(level);
    }

    @Override
    protected void outputLog(@NonNull Level level, @NonNull String message, @Nullable Throwable throwable) {
        System.out.println("[nonop] " + level.name() + " " + message);
        if (throwable != null) {
            throwable.printStackTrace(System.out);
        }
    }

}
