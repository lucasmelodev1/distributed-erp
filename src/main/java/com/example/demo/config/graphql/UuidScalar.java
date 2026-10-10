package com.example.demo.config.graphql;

import java.util.Locale;
import java.util.UUID;

import graphql.GraphQLContext;
import graphql.execution.CoercedVariables;
import graphql.language.StringValue;
import graphql.language.Value;
import graphql.schema.Coercing;
import graphql.schema.CoercingParseLiteralException;
import graphql.schema.CoercingParseValueException;
import graphql.schema.CoercingSerializeException;
import com.netflix.graphql.dgs.DgsScalar;

@DgsScalar(name = "UUID")
public class UuidScalar implements Coercing<UUID, String> {

	@Override
	public String serialize(Object result, GraphQLContext context, Locale locale) {
		if (result instanceof UUID uuid) {
			return uuid.toString();
		}
		throw new CoercingSerializeException("Expected a UUID but was " + result.getClass().getSimpleName());
	}

	@Override
	public UUID parseValue(Object input, GraphQLContext context, Locale locale) {
		if (input instanceof String text) {
			return parse(text, true);
		}
		throw new CoercingParseValueException("Expected a UUID string");
	}

	@Override
	public UUID parseLiteral(Value<?> input, CoercedVariables variables, GraphQLContext context, Locale locale) {
		if (input instanceof StringValue text) {
			return parse(text.getValue(), false);
		}
		throw new CoercingParseLiteralException("Expected a UUID string");
	}

	private static UUID parse(String text, boolean isValue) {
		try {
			return UUID.fromString(text);
		} catch (IllegalArgumentException e) {
			String message = "Invalid UUID: " + text;
			throw isValue ? new CoercingParseValueException(message) : new CoercingParseLiteralException(message);
		}
	}
}
