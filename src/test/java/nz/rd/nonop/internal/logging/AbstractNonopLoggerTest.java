// Copyright 2025 Rich Dougherty <rich@rd.nz>

package nz.rd.nonop.internal.logging;

import nz.rd.nonop.internal.logging.NonopLogger.Level;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AbstractNonopLoggerTest {

    private TestLogger logger;
    private RuntimeException testException;

    @BeforeEach
    void setUp() {
        testException = new RuntimeException("Test exception");
    }

    @Nested
    @DisplayName("When logger level is DEBUG")
    class WhenLoggerLevelIsDebug {

        @BeforeEach
        void setUp() {
            logger = new TestLogger(Level.DEBUG);
        }

        @Test
        @DisplayName("should enable all logging levels")
        void shouldEnableAllLoggingLevels() {
            assertThat(logger.getEnabledLevel(), is(Level.DEBUG));
            assertTrue(logger.isDebugEnabled());
            assertTrue(logger.isInfoEnabled());
            assertTrue(logger.isWarnEnabled());
            assertTrue(logger.isErrorEnabled());
        }

        @Test
        @DisplayName("should call outputLog with DEBUG level for debug message")
        void shouldCallOutputLogWithDebugLevelForDebugMessage() {
            logger.debug("debug message");
            assertThat(logger.getLastLevel(), is(Level.DEBUG));
        }

        @Test
        @DisplayName("should call outputLog with DEBUG level for debug message with exception")
        void shouldCallOutputLogWithDebugLevelForDebugMessageWithException() {
            logger.debug("debug with exception", testException);
            assertThat(logger.getLastLevel(), is(Level.DEBUG));
        }

        @Test
        @DisplayName("should call outputLog with INFO level for info message")
        void shouldCallOutputLogWithInfoLevelForInfoMessage() {
            logger.info("info message");
            assertThat(logger.getLastLevel(), is(Level.INFO));
        }

        @Test
        @DisplayName("should call outputLog with INFO level for info message with exception")
        void shouldCallOutputLogWithInfoLevelForInfoMessageWithException() {
            logger.info("info with exception", testException);
            assertThat(logger.getLastLevel(), is(Level.INFO));
        }

        @Test
        @DisplayName("should call outputLog with WARN level for warn message")
        void shouldCallOutputLogWithWarnLevelForWarnMessage() {
            logger.warn("warn message");
            assertThat(logger.getLastLevel(), is(Level.WARN));
        }

        @Test
        @DisplayName("should call outputLog with WARN level for warn message with exception")
        void shouldCallOutputLogWithWarnLevelForWarnMessageWithException() {
            logger.warn("warn with exception", testException);
            // This will FAIL - expects WARN but gets INFO due to bug
            assertThat(logger.getLastLevel(), is(Level.WARN));
        }

        @Test
        @DisplayName("should call outputLog with ERROR level for error message")
        void shouldCallOutputLogWithErrorLevelForErrorMessage() {
            logger.error("error message");
            assertThat(logger.getLastLevel(), is(Level.ERROR));
        }

        @Test
        @DisplayName("should call outputLog with ERROR level for error message with exception")
        void shouldCallOutputLogWithErrorLevelForErrorMessageWithException() {
            logger.error("error with exception", testException);
            // This will FAIL - expects ERROR but gets INFO due to bug
            assertThat(logger.getLastLevel(), is(Level.ERROR));
        }
    }

    @Nested
    @DisplayName("When logger level is INFO")
    class WhenLoggerLevelIsInfo {

        @BeforeEach
        void setUp() {
            logger = new TestLogger(Level.INFO);
        }

        @Test
        @DisplayName("should enable INFO, WARN, and ERROR levels")
        void shouldEnableAppropriateLoggingLevels() {
            assertThat(logger.getEnabledLevel(), is(Level.INFO));
            assertFalse(logger.isDebugEnabled());
            assertTrue(logger.isInfoEnabled());
            assertTrue(logger.isWarnEnabled());
            assertTrue(logger.isErrorEnabled());
        }

        @Test
        @DisplayName("should not call outputLog for debug message")
        void shouldNotCallOutputLogForDebugMessage() {
            logger.debug("debug message");
            assertFalse(logger.wasOutputLogCalled());
        }

        @Test
        @DisplayName("should not call outputLog for debug message with exception")
        void shouldNotCallOutputLogForDebugMessageWithException() {
            logger.debug("debug with exception", testException);
            assertFalse(logger.wasOutputLogCalled());
        }

        @Test
        @DisplayName("should call outputLog with INFO level for info message")
        void shouldCallOutputLogWithInfoLevelForInfoMessage() {
            logger.info("info message");
            assertThat(logger.getLastLevel(), is(Level.INFO));
        }

        @Test
        @DisplayName("should call outputLog with INFO level for info message with exception")
        void shouldCallOutputLogWithInfoLevelForInfoMessageWithException() {
            logger.info("info with exception", testException);
            assertThat(logger.getLastLevel(), is(Level.INFO));
        }

        @Test
        @DisplayName("should call outputLog with WARN level for warn message")
        void shouldCallOutputLogWithWarnLevelForWarnMessage() {
            logger.warn("warn message");
            assertThat(logger.getLastLevel(), is(Level.WARN));
        }

        @Test
        @DisplayName("should call outputLog with WARN level for warn message with exception")
        void shouldCallOutputLogWithWarnLevelForWarnMessageWithException() {
            logger.warn("warn with exception", testException);
            // This will FAIL - expects WARN but gets INFO due to bug
            assertThat(logger.getLastLevel(), is(Level.WARN));
        }

        @Test
        @DisplayName("should call outputLog with ERROR level for error message")
        void shouldCallOutputLogWithErrorLevelForErrorMessage() {
            logger.error("error message");
            assertThat(logger.getLastLevel(), is(Level.ERROR));
        }

        @Test
        @DisplayName("should call outputLog with ERROR level for error message with exception")
        void shouldCallOutputLogWithErrorLevelForErrorMessageWithException() {
            logger.error("error with exception", testException);
            // This will FAIL - expects ERROR but gets INFO due to bug
            assertThat(logger.getLastLevel(), is(Level.ERROR));
        }
    }

    @Nested
    @DisplayName("When logger level is WARN")
    class WhenLoggerLevelIsWarn {

        @BeforeEach
        void setUp() {
            logger = new TestLogger(Level.WARN);
        }

        @Test
        @DisplayName("should enable only WARN and ERROR levels")
        void shouldEnableAppropriateLoggingLevels() {
            assertThat(logger.getEnabledLevel(), is(Level.WARN));
            assertFalse(logger.isDebugEnabled());
            assertFalse(logger.isInfoEnabled());
            assertTrue(logger.isWarnEnabled());
            assertTrue(logger.isErrorEnabled());
        }

        @Test
        @DisplayName("should not call outputLog for debug message")
        void shouldNotCallOutputLogForDebugMessage() {
            logger.debug("debug message");
            assertFalse(logger.wasOutputLogCalled());
        }

        @Test
        @DisplayName("should not call outputLog for debug message with exception")
        void shouldNotCallOutputLogForDebugMessageWithException() {
            logger.debug("debug with exception", testException);
            assertFalse(logger.wasOutputLogCalled());
        }

        @Test
        @DisplayName("should not call outputLog for info message")
        void shouldNotCallOutputLogForInfoMessage() {
            logger.info("info message");
            assertFalse(logger.wasOutputLogCalled());
        }

        @Test
        @DisplayName("should not call outputLog for info message with exception")
        void shouldNotCallOutputLogForInfoMessageWithException() {
            logger.info("info with exception", testException);
            assertFalse(logger.wasOutputLogCalled());
        }

        @Test
        @DisplayName("should call outputLog with WARN level for warn message")
        void shouldCallOutputLogWithWarnLevelForWarnMessage() {
            logger.warn("warn message");
            assertThat(logger.getLastLevel(), is(Level.WARN));
        }

        @Test
        @DisplayName("should call outputLog with WARN level for warn message with exception")
        void shouldCallOutputLogWithWarnLevelForWarnMessageWithException() {
            logger.warn("warn with exception", testException);
            // This will FAIL - expects WARN but gets INFO due to bug
            assertThat(logger.getLastLevel(), is(Level.WARN));
        }

        @Test
        @DisplayName("should call outputLog with ERROR level for error message")
        void shouldCallOutputLogWithErrorLevelForErrorMessage() {
            logger.error("error message");
            assertThat(logger.getLastLevel(), is(Level.ERROR));
        }

        @Test
        @DisplayName("should call outputLog with ERROR level for error message with exception")
        void shouldCallOutputLogWithErrorLevelForErrorMessageWithException() {
            logger.error("error with exception", testException);
            // This will FAIL - expects ERROR but gets INFO due to bug
            assertThat(logger.getLastLevel(), is(Level.ERROR));
        }
    }

    @Nested
    @DisplayName("When logger level is ERROR")
    class WhenLoggerLevelIsError {

        @BeforeEach
        void setUp() {
            logger = new TestLogger(Level.ERROR);
        }

        @Test
        @DisplayName("should enable only ERROR level")
        void shouldEnableOnlyErrorLevel() {
            assertThat(logger.getEnabledLevel(), is(Level.ERROR));
            assertFalse(logger.isDebugEnabled());
            assertFalse(logger.isInfoEnabled());
            assertFalse(logger.isWarnEnabled());
            assertTrue(logger.isErrorEnabled());
        }

        @Test
        @DisplayName("should not call outputLog for debug message")
        void shouldNotCallOutputLogForDebugMessage() {
            logger.debug("debug message");
            assertFalse(logger.wasOutputLogCalled());
        }

        @Test
        @DisplayName("should not call outputLog for debug message with exception")
        void shouldNotCallOutputLogForDebugMessageWithException() {
            logger.debug("debug with exception", testException);
            assertFalse(logger.wasOutputLogCalled());
        }

        @Test
        @DisplayName("should not call outputLog for info message")
        void shouldNotCallOutputLogForInfoMessage() {
            logger.info("info message");
            assertFalse(logger.wasOutputLogCalled());
        }

        @Test
        @DisplayName("should not call outputLog for info message with exception")
        void shouldNotCallOutputLogForInfoMessageWithException() {
            logger.info("info with exception", testException);
            assertFalse(logger.wasOutputLogCalled());
        }

        @Test
        @DisplayName("should not call outputLog for warn message")
        void shouldNotCallOutputLogForWarnMessage() {
            logger.warn("warn message");
            assertFalse(logger.wasOutputLogCalled());
        }

        @Test
        @DisplayName("should not call outputLog for warn message with exception")
        void shouldNotCallOutputLogForWarnMessageWithException() {
            logger.warn("warn with exception", testException);
            // Even though this has a bug (uses INFO instead of WARN),
            // INFO level is still below ERROR threshold so outputLog won't be called
            assertFalse(logger.wasOutputLogCalled());
        }

        @Test
        @DisplayName("should call outputLog with ERROR level for error message")
        void shouldCallOutputLogWithErrorLevelForErrorMessage() {
            logger.error("error message");
            assertThat(logger.getLastLevel(), is(Level.ERROR));
        }

        @Test
        @DisplayName("should call outputLog with ERROR level for error message with exception")
        void shouldCallOutputLogWithErrorLevelForErrorMessageWithException() {
            logger.error("error with exception", testException);
            // This will FAIL - expects ERROR but gets INFO due to bug
            assertThat(logger.getLastLevel(), is(Level.ERROR));
        }
    }

    @Nested
    @DisplayName("When logger level is OFF")
    class WhenLoggerLevelIsOff {

        @BeforeEach
        void setUp() {
            logger = new TestLogger(Level.OFF);
        }

        @Test
        @DisplayName("should disable all logging levels")
        void shouldDisableAllLoggingLevels() {
            assertThat(logger.getEnabledLevel(), is(Level.OFF));
            assertFalse(logger.isDebugEnabled());
            assertFalse(logger.isInfoEnabled());
            assertFalse(logger.isWarnEnabled());
            assertFalse(logger.isErrorEnabled());
        }

        @Test
        @DisplayName("should not call outputLog for debug message")
        void shouldNotCallOutputLogForDebugMessage() {
            logger.debug("debug message");
            assertFalse(logger.wasOutputLogCalled());
        }

        @Test
        @DisplayName("should not call outputLog for debug message with exception")
        void shouldNotCallOutputLogForDebugMessageWithException() {
            logger.debug("debug with exception", testException);
            assertFalse(logger.wasOutputLogCalled());
        }

        @Test
        @DisplayName("should not call outputLog for info message")
        void shouldNotCallOutputLogForInfoMessage() {
            logger.info("info message");
            assertFalse(logger.wasOutputLogCalled());
        }

        @Test
        @DisplayName("should not call outputLog for info message with exception")
        void shouldNotCallOutputLogForInfoMessageWithException() {
            logger.info("info with exception", testException);
            assertFalse(logger.wasOutputLogCalled());
        }

        @Test
        @DisplayName("should not call outputLog for warn message")
        void shouldNotCallOutputLogForWarnMessage() {
            logger.warn("warn message");
            assertFalse(logger.wasOutputLogCalled());
        }

        @Test
        @DisplayName("should not call outputLog for warn message with exception")
        void shouldNotCallOutputLogForWarnMessageWithException() {
            logger.warn("warn with exception", testException);
            // Even though this has a bug (uses INFO instead of WARN),
            // INFO level is still below OFF threshold so outputLog won't be called
            assertFalse(logger.wasOutputLogCalled());
        }

        @Test
        @DisplayName("should not call outputLog for error message")
        void shouldNotCallOutputLogForErrorMessage() {
            logger.error("error message");
            assertFalse(logger.wasOutputLogCalled());
        }

        @Test
        @DisplayName("should not call outputLog for error message with exception")
        void shouldNotCallOutputLogForErrorMessageWithException() {
            logger.error("error with exception", testException);
            // Even though this has a bug (uses INFO instead of ERROR),
            // INFO level is still below OFF threshold so outputLog won't be called
            assertFalse(logger.wasOutputLogCalled());
        }
    }

    @Test
    @DisplayName("should handle null message correctly")
    void shouldHandleNullMessageCorrectly() {
        logger = new TestLogger(Level.DEBUG);

        logger.debug(null);

        assertTrue(logger.wasOutputLogCalled());
        assertThat(logger.getLastLevel(), is(Level.DEBUG));
        assertThat(logger.getLastMessage(), is(nullValue()));
        assertThat(logger.getLastThrowable(), is(nullValue()));
    }

    // Simple test implementation of AbstractNonopLogger
    private static class TestLogger extends AbstractNonopLogger {
        private boolean outputLogCalled = false;
        private Level lastLevel;
        private String lastMessage;
        private Throwable lastThrowable;

        public TestLogger(Level enabledLevel) {
            super(enabledLevel);
        }

        @Override
        protected void outputLog(@NonNull Level level, @NonNull String message, @Nullable Throwable throwable) {
            this.outputLogCalled = true;
            this.lastLevel = level;
            this.lastMessage = message;
            this.lastThrowable = throwable;
        }

        public boolean wasOutputLogCalled() {
            return outputLogCalled;
        }

        public Level getLastLevel() {
            return lastLevel;
        }

        public String getLastMessage() {
            return lastMessage;
        }

        public Throwable getLastThrowable() {
            return lastThrowable;
        }

        public void reset() {
            this.outputLogCalled = false;
            this.lastLevel = null;
            this.lastMessage = null;
            this.lastThrowable = null;
        }

        @Override
        public void close() {
            // No resources to close for test logger
        }
    }
}