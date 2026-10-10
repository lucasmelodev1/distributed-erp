package com.example.demo.config.graphql;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.graphql.execution.DataFetcherExceptionResolverAdapter;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;

import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import graphql.schema.DataFetchingEnvironment;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ConstraintViolationException;

/**
 * Maps exceptions to GraphQL errors, replacing the REST
 * the REST exception handler it replaced. Unknown failures are logged and returned as a
 * generic error so internals are never exposed.
 */
@Component
class GraphQlExceptionResolver extends DataFetcherExceptionResolverAdapter {
	private static final Logger log = LoggerFactory.getLogger(GraphQlExceptionResolver.class);

	@Override
	protected GraphQLError resolveToSingleError(Throwable ex, DataFetchingEnvironment env) {
		return switch (ex) {
			case EntityNotFoundException e -> error(env, ErrorType.NOT_FOUND, e.getMessage());
			case AuthenticationException e -> error(env, ErrorType.UNAUTHORIZED, e.getMessage());
			case AccessDeniedException e -> error(env, ErrorType.FORBIDDEN, e.getMessage());
			case IllegalArgumentException e -> error(env, ErrorType.BAD_REQUEST, e.getMessage());
			case ConstraintViolationException e -> error(env, ErrorType.BAD_REQUEST, firstViolation(e));
			default -> {
				log.error("Unexpected error", ex);
				yield error(env, ErrorType.INTERNAL_ERROR, "An internal error occurred");
			}
		};
	}

	private static String firstViolation(ConstraintViolationException e) {
		return e.getConstraintViolations().stream()
				.map(v -> v.getPropertyPath() + ": " + v.getMessage())
				.sorted()
				.findFirst()
				.orElse("Validation failed");
	}

	private static GraphQLError error(DataFetchingEnvironment env, ErrorType type, String message) {
		return GraphqlErrorBuilder.newError(env)
				.errorType(type)
				.message(message)
				.extensions(Map.of("code", type.name()))
				.build();
	}
}
