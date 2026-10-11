package com.example.demo;

import java.sql.DriverManager;
import java.sql.SQLException;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.grafana.LgtmStackContainer;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.lifecycle.Startables;
import org.testcontainers.mysql.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

import com.redis.testcontainers.RedisContainer;

/**
 * The containers are static so every Spring context in a test run shares one set, and they are
 * marked reusable so that, with {@code testcontainers.reuse.enable=true} in
 * {@code ~/.testcontainers.properties}, they also survive between runs. Reused containers keep their
 * data, so MySQL and Redis are wiped when the run starts. Kafka keeps its topics and offsets, which is harmless.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

	private static final RedisContainer REDIS = new RedisContainer(DockerImageName.parse("redis:8.6.5-alpine"))
			.withReuse(true);

	private static final LgtmStackContainer GRAFANA = new LgtmStackContainer(
			DockerImageName.parse("grafana/otel-lgtm:latest")).withReuse(true);

	// The same Kafka line and image as docker-compose.yml (and AWS MSK), not the native image.
	private static final KafkaContainer KAFKA = new KafkaContainer(DockerImageName.parse("apache/kafka:4.0.0"))
			.withReuse(true);

	private static final MySQLContainer MYSQL = new MySQLContainer(DockerImageName.parse("mysql:8.0"))
			.withCommand("--character-set-server=utf8mb4", "--collation-server=utf8mb4_0900_ai_ci",
					"--default-time-zone=+00:00")
			.withReuse(true);

	static {
		Startables.deepStart(REDIS, GRAFANA, KAFKA, MYSQL).join();
		resetData();
	}

	private static void resetData() {
		try {
			REDIS.execInContainer("redis-cli", "FLUSHALL");
			try (var connection = DriverManager.getConnection(MYSQL.getJdbcUrl(), "root", MYSQL.getPassword());
					var statement = connection.createStatement()) {
				var database = MYSQL.getDatabaseName();
				statement.execute("DROP DATABASE IF EXISTS `" + database + "`");
				statement.execute("CREATE DATABASE `" + database + "`");
			}
		} catch (SQLException | java.io.IOException e) {
			throw new IllegalStateException("Could not reset the reused test containers", e);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException("Interrupted while resetting the test containers", e);
		}
	}

	@Bean
	@ServiceConnection
	RedisContainer redisContainer() {
		return REDIS;
	}

	@Bean
	@ServiceConnection
	LgtmStackContainer grafanaLgtmContainer() {
		return GRAFANA;
	}

	@Bean
	@ServiceConnection
	KafkaContainer kafkaContainer() {
		return KAFKA;
	}

	@Bean
	@ServiceConnection
	MySQLContainer mysqlContainer() {
		return MYSQL;
	}

}
