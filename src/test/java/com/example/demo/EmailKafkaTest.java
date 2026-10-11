package com.example.demo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.MailSendException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.example.demo.email.EmailService;
import com.example.demo.email.PermanentEmailFailure;

/** The email flow through the real broker: delivery, after-commit publishing, retries and the dead-letter topic. */
class EmailKafkaTest extends BaseGraphQlTest {
	private static final String DEAD_LETTER_TOPIC = "email.send-dlt";
	private static final Duration PRESENT_WAIT = Duration.ofSeconds(15);
	private static final Duration ABSENT_WAIT = Duration.ofSeconds(2);

	@Autowired
	private EmailService emailService;

	@Autowired
	private PlatformTransactionManager transactionManager;

	@Test
	void deliversAnEmailPublishedToKafka() {
		var to = unique();

		emailService.sendSimpleMessage("from@example.com", to, "Hello", "Body");

		var email = sentEmails.awaitTo(to);
		assertThat(email.from()).isEqualTo("from@example.com");
		assertThat(email.subject()).isEqualTo("Hello");
		assertThat(email.text()).isEqualTo("Body");
	}

	@Test
	void publishesOnlyAfterTheTransactionCommits() {
		var rolledBack = unique();
		var committed = unique();
		var transaction = new TransactionTemplate(transactionManager);

		assertThatThrownBy(() -> transaction.executeWithoutResult(status -> {
			emailService.sendSimpleMessage("from@example.com", rolledBack, "Rolled back", "Body");
			throw new IllegalStateException("rollback");
		})).isInstanceOf(IllegalStateException.class);
		transaction.executeWithoutResult(
				status -> emailService.sendSimpleMessage("from@example.com", committed, "Committed", "Body"));

		sentEmails.awaitTo(committed);
		awaitEmailsProcessed();
		assertThat(sentEmails.lastTo(rolledBack)).isEmpty();
	}

	@Test
	void retriesATransientFailureUntilItSucceeds() {
		var to = unique();
		emailSender.failNext(2, () -> new MailSendException("smtp is down"));

		emailService.sendSimpleMessage("from@example.com", to, "Hello", "Body");

		assertThat(sentEmails.awaitTo(to).subject()).isEqualTo("Hello");
		awaitEmailsProcessed();
		assertThat(deadLettered(to, ABSENT_WAIT)).isEmpty();
	}

	@Test
	void deadLettersAnEmailOnceTheRetriesAreExhausted() {
		var to = unique();
		// 4 attempts in total, so a 5th would succeed; it must never get that far.
		emailSender.failNext(10, () -> new MailSendException("smtp is down"));

		emailService.sendSimpleMessage("from@example.com", to, "Hello", "Body");

		assertThat(deadLettered(to, PRESENT_WAIT)).hasSize(1);
		assertThat(sentEmails.lastTo(to)).isEmpty();
	}

	@Test
	void deadLettersAPermanentFailureWithoutRetrying() {
		var to = unique();
		// Only the first send fails: a retry would succeed and deliver the email.
		emailSender.failNext(1, () -> new PermanentEmailFailure("invalid address", null));

		emailService.sendSimpleMessage("from@example.com", to, "Hello", "Body");

		assertThat(deadLettered(to, PRESENT_WAIT)).hasSize(1);
		awaitEmailsProcessed();
		assertThat(sentEmails.lastTo(to)).isEmpty();
	}

	private static String unique() {
		return "kafka-" + UUID.randomUUID() + "@example.com";
	}

	/** Records for this recipient on the dead-letter topic (waiting up to {@code wait} for one to show up). */
	private List<ConsumerRecord<String, String>> deadLettered(String to, Duration wait) {
		var properties = Map.<String, Object>of(
				ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
				kafkaAdmin.getConfigurationProperties().get(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG),
				ConsumerConfig.GROUP_ID_CONFIG, "test-" + UUID.randomUUID(),
				ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest",
				ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
				ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);

		var found = new java.util.ArrayList<ConsumerRecord<String, String>>();
		var deadline = Instant.now().plus(wait);
		try (var consumer = new KafkaConsumer<String, String>(properties)) {
			consumer.subscribe(List.of(DEAD_LETTER_TOPIC));
			while (found.isEmpty() && Instant.now().isBefore(deadline)) {
				consumer.poll(Duration.ofMillis(250)).forEach(record -> {
					if (to.equals(record.key())) {
						found.add(record);
					}
				});
			}
		}
		return found;
	}
}
