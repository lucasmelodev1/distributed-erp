package com.example.demo.config.graphql;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.observation.GraphQlObservationInstrumentation;

import io.micrometer.observation.ObservationRegistry;

/**
 * Records a span and metrics per GraphQL request and data fetcher, tagged with
 * the operation type and name, since every request is otherwise just
 * {@code POST /graphql}.
 */
@Configuration
class GraphQlObservabilityConfig {

	@Bean
	GraphQlObservationInstrumentation graphQlObservationInstrumentation(ObservationRegistry observationRegistry) {
		return new GraphQlObservationInstrumentation(observationRegistry);
	}
}
