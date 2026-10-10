package com.example.demo.config.graphql;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import graphql.analysis.MaxQueryComplexityInstrumentation;
import graphql.analysis.MaxQueryDepthInstrumentation;

/** Protects the single GraphQL endpoint from abusive queries. */
@Configuration
class GraphQlLimitsConfig {

	@Bean
	MaxQueryDepthInstrumentation maxQueryDepthInstrumentation(@Value("${app.graphql.max-depth:10}") int maxDepth) {
		return new MaxQueryDepthInstrumentation(maxDepth);
	}

	@Bean
	MaxQueryComplexityInstrumentation maxQueryComplexityInstrumentation(
			@Value("${app.graphql.max-complexity:200}") int maxComplexity) {
		return new MaxQueryComplexityInstrumentation(maxComplexity);
	}
}
