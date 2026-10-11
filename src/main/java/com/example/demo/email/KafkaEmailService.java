package com.example.demo.email;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/**
 * Publishes emails to Kafka in every environment. Inside a transaction the message is
 * published only after it commits, so a rolled-back change never sends an email.
 */
@Service
public class KafkaEmailService implements EmailService {
	private static final Logger log = LoggerFactory.getLogger(KafkaEmailService.class);

	private final KafkaTemplate<String, String> kafkaTemplate;
	private final JsonMapper jsonMapper;

	public KafkaEmailService(KafkaTemplate<String, String> kafkaTemplate, JsonMapper jsonMapper) {
		this.kafkaTemplate = kafkaTemplate;
		this.jsonMapper = jsonMapper;
	}

	@Override
	public void sendSimpleMessage(String from, String to, @Nullable String subject, @Nullable String text) {
		String payload;
		try {
			payload = jsonMapper.writeValueAsString(new EmailMessage(from, to, subject, text));
		} catch (JacksonException e) {
			throw new IllegalStateException("Could not serialise email", e);
		}

		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
				@Override
				public void afterCommit() {
					publish(to, payload);
				}
			});
		} else {
			publish(to, payload);
		}
	}

	/** Keyed by recipient so emails to one address stay in order. Never throws: the caller already committed. */
	private void publish(String to, String payload) {
		try {
			kafkaTemplate.send(EmailTopics.SEND, to, payload).whenComplete((result, error) -> {
				if (error != null) {
					log.error("Could not publish email to={}: {}", to, error.getMessage(), error);
				}
			});
		} catch (Exception e) {
			log.error("Could not publish email to={}: {}", to, e.getMessage(), e);
		}
	}
}
