package com.example.demo.config.graphql;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Locale;

import graphql.GraphQLContext;
import graphql.execution.CoercedVariables;
import graphql.language.StringValue;
import graphql.language.Value;
import graphql.schema.Coercing;
import graphql.schema.CoercingParseLiteralException;
import graphql.schema.CoercingParseValueException;
import graphql.schema.CoercingSerializeException;
import com.netflix.graphql.dgs.DgsScalar;

/** ISO-8601 instant in UTC, e.g. {@code 2026-10-10T12:00:00Z}. */
@DgsScalar(name = "DateTime")
public class DateTimeScalar implements Coercing<Instant, String> {

	@Override
	public String serialize(Object result, GraphQLContext context, Locale locale) {
		if (result instanceof Instant instant) {
			return instant.toString();
		}
		throw new CoercingSerializeException("Expected an Instant but was " + result.getClass().getSimpleName());
	}

	@Override
	public Instant parseValue(Object input, GraphQLContext context, Locale locale) {
		if (input instanceof String text) {
			try {
				return Instant.parse(text);
			} catch (DateTimeParseException e) {
				throw new CoercingParseValueException("Invalid DateTime: " + text);
			}
		}
		throw new CoercingParseValueException("Expected an ISO-8601 DateTime string");
	}

	@Override
	public Instant parseLiteral(Value<?> input, CoercedVariables variables, GraphQLContext context, Locale locale) {
		if (input instanceof StringValue text) {
			try {
				return Instant.parse(text.getValue());
			} catch (DateTimeParseException e) {
				throw new CoercingParseLiteralException("Invalid DateTime: " + text.getValue());
			}
		}
		throw new CoercingParseLiteralException("Expected an ISO-8601 DateTime string");
	}
}
