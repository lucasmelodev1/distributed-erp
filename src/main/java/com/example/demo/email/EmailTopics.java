package com.example.demo.email;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.config.TopicConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaAdmin;

/**
 * Declares the email topics (and the retry/dead-letter topics Spring Kafka derives from
 * {@link #SEND}) so they get the retention we want instead of the broker default.
 */
@Configuration(proxyBeanMethods = false)
class EmailTopics {
	static final String SEND = "email.send";

	/** Must match the suffixes {@link EmailConsumer} asks Spring Kafka for. */
	static final String DEAD_LETTER = SEND + "-dlt";

	static String retry(int index) {
		return SEND + "-retry-" + index;
	}

	@Bean
	KafkaAdmin.NewTopics emailTopicDeclarations(
			@Value("${app.email.retry.attempts}") int attempts,
			@Value("${app.email.topics.partitions}") int partitions,
			@Value("${app.email.topics.replicas}") short replicas,
			@Value("${app.email.topics.retention}") Duration retention) {
		List<String> names = new ArrayList<>();
		names.add(SEND);
		for (int i = 0; i < attempts - 1; i++) {
			names.add(retry(i));
		}
		names.add(DEAD_LETTER);

		return new KafkaAdmin.NewTopics(names.stream()
				.map(name -> TopicBuilder.name(name)
						.partitions(partitions)
						.replicas(replicas)
						.configs(Map.of(TopicConfig.RETENTION_MS_CONFIG, String.valueOf(retention.toMillis())))
						.build())
				.toArray(NewTopic[]::new));
	}
}
