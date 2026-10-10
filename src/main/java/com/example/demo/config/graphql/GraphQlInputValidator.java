package com.example.demo.config.graphql;

import org.springframework.stereotype.Component;

import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;

/**
 * Applies the Jakarta Bean Validation constraints declared on input records,
 * as {@code @Valid} does for REST request bodies. Violations surface as
 * {@code BAD_REQUEST} errors.
 */
@Component
public class GraphQlInputValidator {
	private final Validator validator;

	GraphQlInputValidator(Validator validator) {
		this.validator = validator;
	}

	public <T> T validate(T input) {
		var violations = validator.validate(input);
		if (!violations.isEmpty()) {
			throw new ConstraintViolationException(violations);
		}
		return input;
	}
}
