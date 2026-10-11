package com.example.demo.config.graphql;

import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient;

import com.example.demo.BaseGraphQlTest;
import com.example.demo.TestUser;

class GraphQlInfrastructureTest extends BaseGraphQlTest {
	private TestUser alpha;
	private TestUser beta;
	private UUID alphaTenantId;

	@BeforeAll
	void setup() {
		alpha = createUser("Gql Alpha", "gql.alpha@mail.com");
		beta = createUser("Gql Beta", "gql.beta@mail.com");
		alphaTenantId = newTenant(alpha, "Gql Alpha Tenant");
	}

	private RestTestClient.ResponseSpec query(String query, TestUser user, String tenantHeader) {
		return graphql(query, Map.of(), user == null ? null : user.cookie().getValue(), tenantHeader);
	}

	private void assertError(RestTestClient.ResponseSpec response, String classification) {
		assertGraphQlError(response, classification);
	}

	@Test
	void publicOperation_withoutAuthentication_succeeds() {
		query("{ ping }", null, null)
				.expectStatus().isOk()
				.expectBody()
				.jsonPath("$.data.ping").isEqualTo(true)
				.jsonPath("$.errors").doesNotExist();
	}

	@Test
	void protectedOperation_withoutAuthentication_isUnauthorized() {
		assertError(query("{ testAuthenticated }", null, null), "UNAUTHORIZED");
	}

	@Test
	void tenantOperation_withInvalidUuid_isBadRequest() {
		assertError(query("{ testTenant }", alpha, "not-a-uuid"), "BAD_REQUEST");
	}

	@Test
	void authenticatedAndTenantOperations_succeedForEntitledCallers() {
		query("{ testAuthenticated }", alpha, null)
				.expectBody().jsonPath("$.data.testAuthenticated").isEqualTo("ok");
		query("{ testCurrentTenant }", alpha, alphaTenantId.toString())
				.expectBody().jsonPath("$.data.testCurrentTenant").isEqualTo(alphaTenantId.toString());
		query("{ testTenantWithPermission }", alpha, alphaTenantId.toString())
				.expectBody().jsonPath("$.data.testTenantWithPermission").isEqualTo("ok");
	}

	@Test
	void exceptions_areMappedAndInternalsAreNotLeaked() {
		assertError(query("{ testThrowsNotFound }", null, null), "NOT_FOUND");
		assertError(query("{ testThrowsIllegalArgument }", null, null), "BAD_REQUEST");
		query("{ testThrowsUnexpected }", null, null)
				.expectBody()
				.jsonPath("$.errors[0].extensions.classification").isEqualTo("INTERNAL_ERROR")
				.jsonPath("$.errors[0].message").isEqualTo("An internal error occurred");
	}

	@Test
	void scalars_roundTrip() {
		var id = UUID.randomUUID();
		query("{ testUuid(value: \"%s\") testNow }".formatted(id), null, null)
				.expectBody()
				.jsonPath("$.data.testUuid").isEqualTo(id.toString())
				.jsonPath("$.data.testNow").isEqualTo("2026-10-10T12:00:00Z");
		query("{ testUuid(value: \"nope\") }", null, null)
				.expectBody()
				.jsonPath("$.errors[0].message").exists();
	}

	@Test
	void queryDeeperThanLimit_isRejected() {
		String nested = "child { ".repeat(12) + "value" + " }".repeat(12);
		query("{ testNode { " + nested + " } }", null, null)
				.expectBody()
				.jsonPath("$.data").doesNotExist()
				.jsonPath("$.errors[0].message").value(msg -> org.junit.jupiter.api.Assertions
						.assertTrue(msg.toString().contains("depth")));
	}

	@Test
	void aliasFloodAboveComplexityLimit_isRejected() {
		var query = new StringBuilder("{ ");
		for (int i = 0; i < 300; i++) {
			query.append("a%d: ping ".formatted(i));
		}
		query.append("}");
		query(query.toString(), null, null)
				.expectBody()
				.jsonPath("$.data").doesNotExist()
				.jsonPath("$.errors[0].message").value(msg -> org.junit.jupiter.api.Assertions
						.assertTrue(msg.toString().contains("complexity")));
	}

	@Test
	void introspection_isDisabledOutsideDev() {
		query("{ __schema { types { name } } }", alpha, null)
				.expectBody()
				.jsonPath("$.data.__schema").doesNotExist()
				.jsonPath("$.errors").exists();
	}

	@Test
	void introspectionFields_areNotSubjectToDefaultDeny() {
		query("{ __typename }", null, null)
				.expectBody()
				.jsonPath("$.errors").doesNotExist()
				.jsonPath("$.data.__typename").isEqualTo("Query");
	}

	@Test
	void formEncodedPost_isRejected() {
		restClient.post()
				.uri("/graphql")
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.body("query={ping}")
				.exchange()
				.expectStatus().is4xxClientError();
	}
}
