package com.example.demo.config.graphql;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Window;

import com.example.demo.config.graphql.Connection.Edge;
import com.example.demo.config.graphql.Connection.PageInfo;

/**
 * Forward cursor pagination (first/after) over keyset scrolling on the id.
 * Ids are UUIDv7, so ordering by id is ordering by creation time and a cursor
 * stays valid while rows are added or removed.
 */
public final class Cursors {
	public static final int DEFAULT_PAGE_SIZE = 20;
	public static final int MAX_PAGE_SIZE = 100;

	private static final String PREFIX = "id:";

	private Cursors() {
	}

	/** Validates {@code first}; defaults to {@link #DEFAULT_PAGE_SIZE}. */
	public static int pageSize(@Nullable Integer first) {
		if (first == null) {
			return DEFAULT_PAGE_SIZE;
		}
		if (first < 1 || first > MAX_PAGE_SIZE) {
			throw new IllegalArgumentException("first must be between 1 and " + MAX_PAGE_SIZE);
		}
		return first;
	}

	public static String encode(UUID id) {
		return Base64.getUrlEncoder().withoutPadding()
				.encodeToString((PREFIX + id).getBytes(StandardCharsets.UTF_8));
	}

	/** Decodes an {@code after} cursor; null/blank means "from the start". */
	public static @Nullable UUID decode(@Nullable String cursor) {
		if (cursor == null || cursor.isBlank()) {
			return null;
		}
		try {
			String text = new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8);
			if (!text.startsWith(PREFIX)) {
				throw new IllegalArgumentException();
			}
			return UUID.fromString(text.substring(PREFIX.length()));
		} catch (IllegalArgumentException e) {
			throw new IllegalArgumentException("Invalid cursor");
		}
	}

	public static <T> Connection<T> toConnection(Window<T> window, Function<T, UUID> idOf, @Nullable String after) {
		List<Edge<T>> edges = window.getContent().stream()
				.map(node -> new Edge<>(node, encode(idOf.apply(node))))
				.toList();
		return new Connection<>(edges, new PageInfo(
				window.hasNext(),
				after != null && !after.isBlank(),
				edges.isEmpty() ? null : edges.getFirst().cursor(),
				edges.isEmpty() ? null : edges.getLast().cursor()));
	}
}
