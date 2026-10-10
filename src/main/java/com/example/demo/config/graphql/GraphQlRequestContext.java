package com.example.demo.config.graphql;

import org.jspecify.annotations.Nullable;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;

import com.example.demo.user.domain.User;

import graphql.schema.DataFetchingEnvironment;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Access to the per-request values captured by
 * {@link GraphQlRequestInterceptor}. Prefer this over thread-locals inside
 * data fetchers.
 */
public final class GraphQlRequestContext {

	private GraphQlRequestContext() {
	}

	/** The authenticated user; fails if the request is anonymous. */
	public static User currentUser(DataFetchingEnvironment environment) {
		Authentication authentication = environment.getGraphQlContext().get(GraphQlContextKeys.AUTHENTICATION);
		if (authentication == null || !(authentication.getPrincipal() instanceof User user)) {
			throw new AuthenticationCredentialsNotFoundException("Authentication required");
		}
		return user;
	}

	/** The authenticated user, or null for an anonymous request (for public operations that adapt to the caller). */
	public static @Nullable User currentUserOrNull(DataFetchingEnvironment environment) {
		Authentication authentication = environment.getGraphQlContext().get(GraphQlContextKeys.AUTHENTICATION);
		return authentication != null && authentication.getPrincipal() instanceof User user ? user : null;
	}

	public static HttpServletRequest httpRequest(DataFetchingEnvironment environment) {
		HttpServletRequest request = environment.getGraphQlContext().get(GraphQlContextKeys.HTTP_REQUEST);
		if (request == null) {
			throw new IllegalStateException("No HTTP request is bound to this GraphQL execution");
		}
		return request;
	}

	public static HttpServletResponse httpResponse(DataFetchingEnvironment environment) {
		HttpServletResponse response = environment.getGraphQlContext().get(GraphQlContextKeys.HTTP_RESPONSE);
		if (response == null) {
			throw new IllegalStateException("No HTTP response is bound to this GraphQL execution");
		}
		return response;
	}
}
