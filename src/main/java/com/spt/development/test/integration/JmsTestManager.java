package com.spt.development.test.integration;

import jakarta.jms.JMSException;
import jakarta.jms.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.support.JmsUtils;
import org.springframework.jms.support.converter.MessageConverter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.springframework.jms.support.destination.JmsDestinationAccessor.RECEIVE_TIMEOUT_NO_WAIT;

/**
 * Wrapper around {@link JmsTemplate} to simplify consuming messages from a JMS queue.
 */
public class JmsTestManager {
    /**
     * The amount of time in ms to wait for messages to be received on the JMS queue, when messages are expected.
     */
    public static final int ASYNC_TEST_TIMEOUT_MS = 30000;

    private static final Logger LOG = LoggerFactory.getLogger(JmsTestManager.class);

    private final JmsTemplate jmsTemplate;

    /**
     * Creates a test manager instance.
     *
     * @param jmsTemplate the {@link JmsTemplate} to use for connecting to the JMS queue to receive messages from.
     */
    public JmsTestManager(final JmsTemplate jmsTemplate) {
        this.jmsTemplate = jmsTemplate;
    }

    /**
     * Consumes all messages on the queue until no more messages are read.
     *
     * @param queue the queue to consume the messages from.
     *
     * @return a list of the message [bodies].
     */
    public List<Object> consumeAllMessagesOnQueue(String queue) {
        return consumeAllMessagesOnQueue(queue, false);
    }

    /**
     * Consumes all messages on the queue until no more messages are read, optionally waiting ASYNC_TEST_TIMEOUT_MS ms
     * before the first one can be read, if messages are expected to be on the queue.
     *
     * @param queue the queue to consume the messages from.
     * @param messagesExpected a flag to determine whether messages should be expected to arrive on the queue or not.
     *
     * @return a list of the message [bodies].
     */
    public List<Object> consumeAllMessagesOnQueue(String queue, boolean messagesExpected) {
        final long originalTimeout = jmsTemplate.getReceiveTimeout();

        try {
            return doConsumeAllMessagesOnQueue(queue, messagesExpected);
        } finally {
            jmsTemplate.setReceiveTimeout(originalTimeout);
        }
    }

    private List<Object> doConsumeAllMessagesOnQueue(String queue, boolean messagesExpected) {
        final Object message = receiveAndConvert(queue, messagesExpected);

        if (message == null) {
            if (messagesExpected) {
                LOG.warn("Expected to find messages on the queue: '{}' when clearing it, but none were found", queue);
            }
            return Collections.emptyList();
        }
        return consumeRemainingMessages(queue, messagesExpected, message);
    }

    private List<Object> consumeRemainingMessages(String queue, boolean messagesExpected, Object firstMessage) {
        final List<Object> messages = new ArrayList<>(Collections.singletonList(firstMessage));

        if (!messagesExpected) {
            LOG.warn(
                "Found message with contents: {} on queue: '{}' when clearing it. This usually indicates a previous test failed",
                firstMessage, queue
            );
        }
        jmsTemplate.setReceiveTimeout(RECEIVE_TIMEOUT_NO_WAIT);

        for (Object m = jmsTemplate.receive(queue); m != null; m = jmsTemplate.receive(queue)) {
            messages.add(m);
        }
        return messages;
    }

    /**
     * Receives a single message from the queue if there is a message to receieve.
     *
     * @param queue the queue to receive the message from.
     *
     * @return the message [body].
     */
    public Object receiveAndConvert(String queue) {
        return receiveAndConvert(queue, false);
    }

    /**
     * Receives a single message from the queue, optionally waiting ASYNC_TEST_TIMEOUT_MS ms to successfully read a
     * message, if messages are expected to be on the queue.
     *
     * @param queue the queue to consume the messages from.
     *
     * @param messagesExpected a flag to determine whether messages should be expected to arrive on the queue or not.
     *
     * @return the message [body].
     */
    public Object receiveAndConvert(String queue, boolean messagesExpected) {
        final long originalTimeout = jmsTemplate.getReceiveTimeout();

        try {
            jmsTemplate.setReceiveTimeout(messagesExpected ? ASYNC_TEST_TIMEOUT_MS : RECEIVE_TIMEOUT_NO_WAIT);

            if (messagesExpected) {
                LOG.info("Receiving message on queue: '{}'. Messages expected, so waiting up to {}ms for message",
                        queue, ASYNC_TEST_TIMEOUT_MS);
            }

            return doReceiveAndConvert(queue);
        } finally {
            jmsTemplate.setReceiveTimeout(originalTimeout);
        }
    }

    private Object doReceiveAndConvert(String queue) {
        final Message message = jmsTemplate.receive(queue);

        if (message != null) {
            LOG.debug("Received message: {} on queue: '{}'", message, queue);

            return doConvertFromMessage(message);
        }
        return null;
    }

    private Object doConvertFromMessage(Message message) {
        return Optional.ofNullable(jmsTemplate.getMessageConverter())
                .map(c -> fromMessage(c, message))
                .orElseThrow(() -> new IllegalStateException("jmsTemplate must havd a message converter"));
    }

    private Object fromMessage(MessageConverter converter, Message message) {

        try {
            return converter.fromMessage(message);
        } catch (JMSException ex) {
            throw JmsUtils.convertJmsAccessException(ex);
        }
    }
}
