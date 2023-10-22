package com.spt.development.test;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Utility methods for verifying that messages are logged (with Logback).
 */
public final class LogbackUtil {
    private LogbackUtil() {}

    /**
     * Verifies that calling the {@link Callable} action results in a message being logged at INFO level, containing
     * the expectedMessageParts. The first message logged at INFO is used for verification, all other log messages
     * are excluded - if multiple messages are expected to be logged at INFO, use
     * {@link LogbackUtil#verifyLogging(Class, Callable, Consumer)}.
     *
     * @param clazz the class that the method under test belongs to.
     * @param act the action to call to perform the test that is expected to result in logging.
     * @param expectedMessageParts {@link String}s that are expected to be contained in the log message.
     * @param <T> the return type of the method under test.
     *
     * @return the result of calling the method under test.
     */
    public static <T> T verifyInfoLogging(Class<?> clazz, Callable<T> act, String... expectedMessageParts) {
        return verifyLogging(clazz, act, Level.INFO, expectedMessageParts);
    }

    /**
     * Verifies that calling the {@link Callable} action results in a message being logged at WARN level, containing
     * the expectedMessageParts. The first message logged at WARN is used for verification, all other log messages
     * are excluded - if multiple messages are expected to be logged at WARN, use
     * {@link LogbackUtil#verifyLogging(Class, Callable, Consumer)}.
     *
     * @param clazz the class that the method under test belongs to.
     * @param act the action to call to perform the test that is expected to result in logging.
     * @param expectedMessageParts {@link String}s that are expected to be contained in the log message.
     * @param <T> the return type of the method under test.
     *
     * @return the result of calling the method under test.
     */
    public static <T> T verifyWarnLogging(Class<?> clazz, Callable<T> act, String... expectedMessageParts) {
        return verifyLogging(clazz, act, Level.WARN, expectedMessageParts);
    }

    /**
     * Verifies that calling the {@link Callable} action results in a message being logged at ERROR level, containing
     * the expectedMessageParts. The first message logged at ERROR is used for verification, all other log messages
     * are excluded - if multiple messages are expected to be logged at ERROR, use
     * {@link LogbackUtil#verifyLogging(Class, Callable, Consumer)}.
     *
     * @param clazz the class that the method under test belongs to.
     * @param act the action to call to perform the test that is expected to result in logging.
     * @param expectedMessageParts {@link String}s that are expected to be contained in the log message.
     * @param <T> the return type of the method under test.
     *
     * @return the result of calling the method under test.
     */
    public static <T> T verifyErrorLogging(Class<?> clazz, Callable<T> act, String... expectedMessageParts) {
        return verifyLogging(clazz, act, Level.ERROR, expectedMessageParts);
    }

    /**
     * Verifies that calling the {@link Callable} action results in a message being logged at the given level, containing
     * the expectedMessageParts. The first message logged at the given level is used for verification, all other log
     * messages are excluded - if multiple messages are expected to be logged at the given level, use
     * {@link LogbackUtil#verifyLogging(Class, Callable, Consumer)}.
     *
     * @param clazz the class that the method under test belongs to.
     * @param act the action to call to perform the test that is expected to result in logging.
     * @param level the level that the log messages to verify are expected to be logged at.
     * @param expectedMessageParts {@link String}s that are expected to be contained in the log message.
     * @param <T> the return type of the method under test.
     *
     * @return the result of calling the method under test.
     */
    public static <T> T verifyLogging(Class<?> clazz, Callable<T> act, Level level, String... expectedMessageParts) {
        return verifyLogging(clazz, act, logs -> {
            final ILoggingEvent logEvent = logs.stream()
                    .filter(e -> e.getLevel() == level)
                    .findFirst()
                    .orElse(null);

            assertThat(logEvent, is(notNullValue()));

            for (String expectedLogPart : expectedMessageParts) {
                assertThat(logEvent.getFormattedMessage(), containsString(expectedLogPart));
            }
        });
    }

    /**
     * Verifies that calling the {@link Callable} action results in at least one message being logged. All messages
     * are then passed to asserts to perform further assertions on them.
     *
     * @param clazz the class that the method under test belongs to.
     * @param act the action to call to perform the test that is expected to result in logging.
     * @param asserts a delegate to perform assertions against the logged messages.
     * @param <T> the return type of the method under test.
     *
     * @return the result of calling the method under test.
     */
    public static <T> T verifyLogging(Class<?> clazz, Callable<T> act, Consumer<List<ILoggingEvent>> asserts) {
        final Appender<ILoggingEvent> appender = createMockAppender();

        final LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        loggerContext.getLogger(clazz).addAppender(appender);

        final ArgumentCaptor<ILoggingEvent> eventCaptor = ArgumentCaptor.forClass(ILoggingEvent.class);

        try {
            final T result = act.call();

            verify(appender, atLeastOnce()).doAppend(eventCaptor.capture());

            asserts.accept(eventCaptor.getAllValues());

            return result;
        } catch (Throwable t) {
            return fail(t);
        } finally {
            loggerContext.getLogger(clazz).detachAppender(appender);
        }
    }

    /**
     * Verifies that calling the {@link Callable} action results in no messages being logged.
     *
     * @param clazz the class that the method under test belongs to.
     * @param act the action to call to perform the test that is expected to result in no logging.
     * @param <T> the return type of the method under test.
     *
     * @return the result of calling the method under test.
     */
    public static <T> T verifyNoLogging(Class<?> clazz, Callable<T> act) {
        final Appender<ILoggingEvent> appender = createMockAppender();

        final LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        loggerContext.getLogger(clazz).addAppender(appender);

        final ArgumentCaptor<ILoggingEvent> eventCaptor = ArgumentCaptor.forClass(ILoggingEvent.class);

        try {
            final T result = act.call();

            verify(appender, never()).doAppend(eventCaptor.capture());

            return result;
        } catch (Throwable t) {
            return fail(t);
        } finally {
            loggerContext.getLogger(clazz).detachAppender(appender);
        }
    }

    @SuppressWarnings("unchecked")
    private static Appender<ILoggingEvent> createMockAppender() {
        return Mockito.mock(Appender.class);
    }
}
