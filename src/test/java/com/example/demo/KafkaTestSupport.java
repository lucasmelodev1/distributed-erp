package com.example.demo;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.OffsetSpec;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.springframework.kafka.core.KafkaAdmin;

/** Helpers for tests that go through the real Kafka email pipeline. */
public final class KafkaTestSupport {
	private static final String EMAIL_GROUP = "demo-email";
	private static final String EMAIL_TOPIC_PREFIX = "email.send";
	private static final Duration TIMEOUT = Duration.ofSeconds(20);

	private KafkaTestSupport() {
	}

	/** Blocks until the email consumer group has committed every record of the email topics. */
	public static void awaitEmailsProcessed(KafkaAdmin kafkaAdmin) {
		var deadline = Instant.now().plus(TIMEOUT);
		try (Admin admin = Admin.create(kafkaAdmin.getConfigurationProperties())) {
			while (lag(admin) > 0) {
				if (Instant.now().isAfter(deadline)) {
					throw new IllegalStateException("Emails were not processed within " + TIMEOUT);
				}
				Thread.sleep(25);
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException("Interrupted while waiting for emails", e);
		} catch (ExecutionException e) {
			throw new IllegalStateException("Could not read the email consumer lag", e);
		}
	}

	private static long lag(Admin admin) throws ExecutionException, InterruptedException {
		var topics = admin.listTopics().names().get().stream()
				.filter(name -> name.startsWith(EMAIL_TOPIC_PREFIX))
				.toList();
		if (topics.isEmpty()) {
			return 0;
		}

		Map<TopicPartition, OffsetSpec> latest = new HashMap<>();
		for (var description : admin.describeTopics(topics).allTopicNames().get().values()) {
			description.partitions().forEach(
					partition -> latest.put(new TopicPartition(description.name(), partition.partition()),
							OffsetSpec.latest()));
		}

		Map<TopicPartition, OffsetAndMetadata> committed = admin.listConsumerGroupOffsets(EMAIL_GROUP)
				.partitionsToOffsetAndMetadata().get();
		Map<TopicPartition, OffsetSpec> earliest = new HashMap<>();
		latest.keySet().forEach(partition -> earliest.put(partition, OffsetSpec.earliest()));
		var start = admin.listOffsets(earliest).all().get();

		long lag = 0;
		for (var end : admin.listOffsets(latest).all().get().entrySet()) {
			var done = committed.get(end.getKey());
			// Nothing committed yet: the consumer starts at the earliest record still retained.
			var position = done == null ? start.get(end.getKey()).offset() : done.offset();
			lag += end.getValue().offset() - position;
		}
		return lag;
	}
}
