package com.example.demo.config.graphql;

import java.util.UUID;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import com.example.demo.tenant.config.AccessSnapshot;
import com.example.demo.tenant.config.TenantAccessChecker;
import com.example.demo.tenant.config.TenantAware;
import com.example.demo.tenant.config.TenantContext;
import com.example.demo.user.domain.User;

import graphql.GraphQLContext;
import graphql.execution.instrumentation.InstrumentationState;
import graphql.execution.instrumentation.SimplePerformantInstrumentation;
import graphql.execution.instrumentation.parameters.InstrumentationFieldFetchParameters;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLTypeUtil;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Enforces authentication, tenant membership and permissions on every
 * top-level Query/Mutation field before its data fetcher runs.
 *
 * <ul>
 * <li>Default-deny: authentication is required unless the operation is marked
 * {@link PublicOperation}.</li>
 * <li>{@link TenantAware} operations require a valid {@code X-Tenant-Id} the
 * user belongs to, plus every permission listed in {@code withPermissions}.</li>
 * <li>{@link TenantContext} is only set while the fetcher runs.</li>
 * </ul>
 */
@Component
class GraphQlAccessInstrumentation extends SimplePerformantInstrumentation {
	private final GraphQlOperationRegistry registry;
	private final TenantAccessChecker accessChecker;

	GraphQlAccessInstrumentation(GraphQlOperationRegistry registry, TenantAccessChecker accessChecker) {
		this.registry = registry;
		this.accessChecker = accessChecker;
	}

	@Override
	public DataFetcher<?> instrumentDataFetcher(DataFetcher<?> dataFetcher,
			InstrumentationFieldFetchParameters parameters, InstrumentationState state) {
		if (parameters.getExecutionStepInfo().getPath().getLevel() != 1) {
			return dataFetcher;
		}
		// Introspection fields (__schema, __type, __typename) are not operations; whether
		// they work at all is decided by spring.graphql.schema.introspection.enabled.
		if (parameters.getField().getName().startsWith("__")) {
			return dataFetcher;
		}
		return environment -> {
			try {
				authorize(environment);
				return dataFetcher.get(environment);
			} finally {
				TenantContext.clear();
			}
		};
	}

	private void authorize(DataFetchingEnvironment environment) {
		String parentType = GraphQLTypeUtil.simplePrint(environment.getParentType());
		var policy = registry.policyFor(parentType, environment.getField().getName());
		if (policy.isPublic()) {
			return;
		}

		GraphQLContext context = environment.getGraphQlContext();
		User user = authenticatedUser(context);

		TenantAware tenantAware = policy.tenantAware();
		if (tenantAware == null) {
			return;
		}

		var tenant = resolveTenant(context, user);
		var decision = accessChecker.check(tenant.snapshot(), tenant.id(), tenantAware);
		if (!decision.granted()) {
			throw new AccessDeniedException(decision.message());
		}
		TenantContext.setTenantId(tenant.id());
	}

	private User authenticatedUser(GraphQLContext context) {
		Authentication authentication = context.get(GraphQlContextKeys.AUTHENTICATION);
		if (authentication == null || !authentication.isAuthenticated()
				|| !(authentication.getPrincipal() instanceof User user)) {
			throw new AuthenticationCredentialsNotFoundException("Authentication required");
		}
		return user;
	}

	/** The tenant a request selected and the caller's access snapshot. */
	private record SelectedTenant(UUID id, AccessSnapshot snapshot) {
	}

	/** Parses the tenant header and reads the access snapshot once per request, caching both. */
	private SelectedTenant resolveTenant(GraphQLContext context, User user) {
		SelectedTenant cached = context.get(GraphQlContextKeys.SELECTED_TENANT);
		if (cached != null) {
			return cached;
		}

		String header = context.get(GraphQlContextKeys.TENANT_HEADER);
		if (header == null || header.isBlank()) {
			throw new AuthenticationCredentialsNotFoundException(
					"Missing required header: " + TenantContext.TENANT_ID_HEADER);
		}

		UUID tenantId;
		try {
			tenantId = UUID.fromString(header);
		} catch (IllegalArgumentException e) {
			throw new IllegalArgumentException(
					"Invalid UUID format for header: " + TenantContext.TENANT_ID_HEADER);
		}

		HttpServletRequest request = context.get(GraphQlContextKeys.HTTP_REQUEST);
		if (request == null) {
			throw new IllegalStateException("No HTTP request is bound to this GraphQL execution");
		}
		var selected = new SelectedTenant(tenantId, accessChecker.snapshot(request, user.getId()));
		context.put(GraphQlContextKeys.SELECTED_TENANT, selected);
		return selected;
	}
}
