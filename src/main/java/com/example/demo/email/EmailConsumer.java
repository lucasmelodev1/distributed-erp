package com.example.demo.email;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.BackOff;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.retrytopic.TopicSuffixingStrategy;
import org.springframework.stereotype.Component;

import io.micrometer.core.instrument.MeterRegistry;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/**
 * Sends the emails published to {@link EmailTopics#SEND}.
 * <p>
 * Failures are retried without blocking the topic: each attempt moves the message to the next
 * {@code email.send-retry-N} topic with a growing delay, and after the last one it lands in
 * {@code email.send-dlt}. A {@link PermanentEmailFailure} skips the retries and goes straight there.
 */
@Component
class EmailConsumer {
	private static final Logger log = LoggerFactory.getLogger(EmailConsumer.class);

	private final EmailSender emailSender;
	private final JsonMapper jsonMapper;
	private final MeterRegistry meterRegistry;

	EmailConsumer(EmailSender emailSender, JsonMapper jsonMapper, MeterRegistry meterRegistry) {
		this.emailSender = emailSender;
		this.jsonMapper = jsonMapper;
		this.meterRegistry = meterRegistry;
	}

	@RetryableTopic(
			attempts = "${app.email.retry.attempts}",
			backOff = @BackOff(
					delayString = "${app.email.retry.delay}",
					multiplierString = "${app.email.retry.multiplier}"),
			exclude = PermanentEmailFailure.class,
			traversingCauses = "true",
			topicSuffixingStrategy = TopicSuffixingStrategy.SUFFIX_WITH_INDEX_VALUE,
			autoCreateTopics = "false")
	@KafkaListener(topics = EmailTopics.SEND)
	void send(String payload) {
		EmailMessage message;
		try {
			message = jsonMapper.readValue(payload, EmailMessage.class);
		} catch (JacksonException e) {
			throw new PermanentEmailFailure("Unreadable email message", e);
		}

		emailSender.send(message);
		meterRegistry.counter("email.sent").increment();
	}

	/** Runs once the retries are exhausted or the failure was permanent. Never logs the body (it holds codes). */
	@DltHandler
	void deadLetter(String payload) {
		meterRegistry.counter("email.dead_lettered").increment();
		log.error("Email dead-lettered to {}; inspect the topic to replay it", EmailTopics.DEAD_LETTER);
	}
}
