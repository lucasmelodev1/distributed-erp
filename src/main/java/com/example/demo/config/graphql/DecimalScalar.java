package com.example.demo.config.graphql;

import java.math.BigDecimal;
import java.util.Locale;

import graphql.GraphQLContext;
import graphql.execution.CoercedVariables;
import graphql.language.IntValue;
import graphql.language.StringValue;
import graphql.language.Value;
import graphql.schema.Coercing;
import graphql.schema.CoercingParseLiteralException;
import graphql.schema.CoercingParseValueException;
import graphql.schema.CoercingSerializeException;
import com.netflix.graphql.dgs.DgsScalar;

/** Lossless decimal: a {@link BigDecimal} carried as a string. Floating point numbers are rejected. */
@DgsScalar(name = "Decimal")
public class DecimalScalar implements Coercing<BigDecimal, String> {

	@Override
	public String serialize(Object result, GraphQLContext context, Locale locale) {
		if (result instanceof BigDecimal decimal) {
			return decimal.toPlainString();
		}
		throw new CoercingSerializeException("Expected a BigDecimal but was " + result.getClass().getSimpleName());
	}

	@Override
	public BigDecimal parseValue(Object input, GraphQLContext context, Locale locale) {
		if (input instanceof String text) {
			return parse(text, true);
		}
		if (input instanceof Integer || input instanceof Long) {
			return BigDecimal.valueOf(((Number) input).longValue());
		}
		throw new CoercingParseValueException("Expected a decimal string");
	}

	@Override
	public BigDecimal parseLiteral(Value<?> input, CoercedVariables variables, GraphQLContext context, Locale locale) {
		if (input instanceof StringValue text) {
			return parse(text.getValue(), false);
		}
		if (input instanceof IntValue integer) {
			return new BigDecimal(integer.getValue());
		}
		throw new CoercingParseLiteralException("Expected a decimal string");
	}

	private static BigDecimal parse(String text, boolean isValue) {
		try {
			return new BigDecimal(text.trim());
		} catch (NumberFormatException e) {
			String message = "Invalid Decimal: " + text;
			throw isValue ? new CoercingParseValueException(message) : new CoercingParseLiteralException(message);
		}
	}
}
