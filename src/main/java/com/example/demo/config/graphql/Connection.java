package com.example.demo.config.graphql;

import java.util.List;

import org.jspecify.annotations.Nullable;

/** Relay-style cursor connection returned by paginated list fields. */
public record Connection<T>(List<Edge<T>> edges, PageInfo pageInfo) {

	public record Edge<T>(T node, String cursor) {
	}

	public record PageInfo(
			boolean hasNextPage,
			boolean hasPreviousPage,
			@Nullable String startCursor,
			@Nullable String endCursor) {
	}
}
