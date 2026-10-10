package com.example.demo.config.graphql;

import java.util.HashMap;
import java.util.Map;

import org.springframework.graphql.server.WebGraphQlInterceptor;
import org.springframework.graphql.server.WebGraphQlRequest;
import org.springframework.graphql.server.WebGraphQlResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.example.demo.tenant.config.TenantValidationInterceptor;

import reactor.core.publisher.Mono;

/**
 * Copies the authenticated user, the tenant header and the servlet
 * request/response from the HTTP request thread into the GraphQL context, so data fetchers never depend on
 * thread-locals that may not follow the execution.
 */
@Component
class GraphQlRequestInterceptor implements WebGraphQlInterceptor {

	@Override
	public Mono<WebGraphQlResponse> intercept(WebGraphQlRequest request, Chain chain) {
		Map<String, Object> values = new HashMap<>();

		var authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication != null) {
			values.put(GraphQlContextKeys.AUTHENTICATION, authentication);
		}

		if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
			values.put(GraphQlContextKeys.HTTP_REQUEST, attributes.getRequest());
			if (attributes.getResponse() != null) {
				values.put(GraphQlContextKeys.HTTP_RESPONSE, attributes.getResponse());
			}
		}

		String tenantHeader = request.getHeaders().getFirst(TenantValidationInterceptor.TENANT_ID_HEADER);
		if (tenantHeader != null) {
			values.put(GraphQlContextKeys.TENANT_HEADER, tenantHeader);
		}

		request.configureExecutionInput((input, builder) -> builder.graphQLContext(values).build());
		return chain.next(request);
	}
}
