package com.example.demo.config.graphql;

/** Keys of the values stored in the per-request {@code GraphQLContext}. */
final class GraphQlContextKeys {
	static final String AUTHENTICATION = "app.authentication";
	static final String TENANT_HEADER = "app.tenantHeader";
	static final String SELECTED_TENANT = "app.selectedTenant";
	static final String HTTP_REQUEST = "app.httpRequest";
	static final String HTTP_RESPONSE = "app.httpResponse";

	private GraphQlContextKeys() {
	}
}
