package com.example.demo;

import java.util.Map;

import org.jspecify.annotations.Nullable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient;

/** Base for tests that call the GraphQL endpoint over HTTP. */
public abstract class BaseGraphQlTest extends BaseControllerTest {

	protected BaseGraphQlTest() {
		super("");
	}

	/** Anonymous request. */
	protected RestTestClient.ResponseSpec graphql(String document) {
		return graphql(document, Map.of(), null, null);
	}

	protected RestTestClient.ResponseSpec graphql(String document, Map<String, Object> variables) {
		return graphql(document, variables, null, null);
	}

	protected RestTestClient.ResponseSpec graphqlAs(String document, Map<String, Object> variables, TestUser user) {
		return graphql(document, variables, user.cookie().getValue(), null);
	}

	protected RestTestClient.ResponseSpec graphql(String document, Map<String, Object> variables,
			@Nullable String sessionId, @Nullable String tenantHeader) {
		var request = restClient.post()
				.uri("/graphql")
				.contentType(MediaType.APPLICATION_JSON)
				.accept(MediaType.APPLICATION_JSON);
		if (sessionId != null) {
			request = request.cookie(SESSION_NAME, sessionId);
		}
		if (tenantHeader != null) {
			request = request.header(TENANT_HEADER, tenantHeader);
		}
		return request.body(Map.of("query", document, "variables", variables)).exchange();
	}

	/** Builds a variables map; null values are kept (unlike Map.of). */
	protected static Map<String, Object> variables(Object... keyValues) {
		var map = new java.util.HashMap<String, Object>();
		for (int i = 0; i < keyValues.length; i += 2) {
			map.put((String) keyValues[i], keyValues[i + 1]);
		}
		return map;
	}

	/** Request as a member of a tenant: sends the session cookie and the X-Tenant-Id header. */
	protected RestTestClient.ResponseSpec graphqlIn(java.util.UUID tenantId, TestUser user, String document,
			Map<String, Object> variables) {
		return graphql(document, variables, user.cookie().getValue(), tenantId.toString());
	}

	protected java.util.UUID newTenant(TestUser user, String name) {
		var id = new java.util.UUID[1];
		graphqlAs("mutation($input: CreateTenantInput!) { createTenant(input: $input) { tenant { id } } }",
				variables("input", variables("name", name)), user)
				.expectBody()
				.jsonPath("$.errors").doesNotExist()
				.jsonPath("$.data.createTenant.tenant.id").value(v -> id[0] = java.util.UUID.fromString(v.toString()));
		return id[0];
	}

	protected java.util.UUID newRole(java.util.UUID tenantId, TestUser user, String name,
			java.util.List<String> permissionCodes) {
		var id = new java.util.UUID[1];
		graphqlIn(tenantId, user, """
				mutation($input: CreateTenantRoleInput!) { createTenantRole(input: $input) { tenantRole { id } } }""",
				variables("input", variables("name", name, "permissionCodes", permissionCodes)))
				.expectBody()
				.jsonPath("$.errors").doesNotExist()
				.jsonPath("$.data.createTenantRole.tenantRole.id")
				.value(v -> id[0] = java.util.UUID.fromString(v.toString()));
		return id[0];
	}

	/** GraphQL reports failures in the body with HTTP 200. */
	protected void assertGraphQlError(RestTestClient.ResponseSpec response, String classification) {
		response.expectStatus().isOk()
				.expectBody()
				.jsonPath("$.errors[0].extensions.classification").isEqualTo(classification)
				.jsonPath("$.errors[0].extensions.code").isEqualTo(classification);
	}
}
