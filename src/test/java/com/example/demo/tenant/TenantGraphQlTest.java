package com.example.demo.tenant;

import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.client.RestTestClient;

import com.example.demo.BaseGraphQlTest;
import com.example.demo.TestUser;

class TenantGraphQlTest extends BaseGraphQlTest {
	private static final String CREATE = """
			mutation($input: CreateTenantInput!) { createTenant(input: $input) { tenant { id name } } }""";
	private static final String UPDATE = """
			mutation($input: UpdateTenantInput!) { updateTenant(input: $input) { tenant { id name } } }""";
	private static final String CURRENT = "{ currentTenant { id name } }";
	private static final String MINE = "{ myTenants { id name active } }";
	private static final String INACTIVATE = """
			mutation { inactivateTenant { tenant { id active inactivatedAt } } }""";
	private static final String REACTIVATE = """
			mutation { reactivateTenant { tenant { id active inactivatedAt } } }""";

	private TestUser alpha;
	private TestUser beta;

	@BeforeAll
	void setup() {
		alpha = createUser("Tenant Alpha", "gql.tenant.alpha@mail.com");
		beta = createUser("Tenant Beta", "gql.tenant.beta@mail.com");
	}

	private static Map<String, Object> input(String name) {
		return Map.of("input", Map.of("name", name));
	}

	private UUID createTenantAs(TestUser user, String name) {
		var id = new UUID[1];
		graphqlAs(CREATE, input(name), user)
				.expectStatus().isOk()
				.expectBody()
				.jsonPath("$.errors").doesNotExist()
				.jsonPath("$.data.createTenant.tenant.name").isEqualTo(name)
				.jsonPath("$.data.createTenant.tenant.id").value(value -> id[0] = UUID.fromString(value.toString()));
		return id[0];
	}

	private RestTestClient.ResponseSpec asMemberOf(String document, Map<String, Object> variables, TestUser user,
			UUID tenantId) {
		return graphql(document, variables, user.cookie().getValue(), tenantId.toString());
	}

	@Test
	void fullFlow_createListReadUpdateDelete() {
		var tenantId = createTenantAs(alpha, "Alpha Tenant");

		graphqlAs(MINE, Map.of(), alpha)
				.expectBody()
				.jsonPath("$.errors").doesNotExist()
				.jsonPath("$.data.myTenants[?(@.id == '%s')].name".formatted(tenantId)).isEqualTo("Alpha Tenant");

		asMemberOf(CURRENT, Map.of(), alpha, tenantId)
				.expectBody()
				.jsonPath("$.errors").doesNotExist()
				.jsonPath("$.data.currentTenant.id").isEqualTo(tenantId.toString())
				.jsonPath("$.data.currentTenant.name").isEqualTo("Alpha Tenant");

		asMemberOf(UPDATE, input("New Alpha Tenant"), alpha, tenantId)
				.expectBody()
				.jsonPath("$.errors").doesNotExist()
				.jsonPath("$.data.updateTenant.tenant.name").isEqualTo("New Alpha Tenant");
		asMemberOf(CURRENT, Map.of(), alpha, tenantId)
				.expectBody()
				.jsonPath("$.data.currentTenant.name").isEqualTo("New Alpha Tenant");

		asMemberOf(INACTIVATE, Map.of(), alpha, tenantId)
				.expectBody()
				.jsonPath("$.errors").doesNotExist()
				.jsonPath("$.data.inactivateTenant.tenant.active").isEqualTo(false)
				.jsonPath("$.data.inactivateTenant.tenant.inactivatedAt").isNotEmpty();
		graphqlAs(MINE, Map.of(), alpha)
				.expectBody()
				.jsonPath("$.errors").doesNotExist()
				.jsonPath("$.data.myTenants[?(@.id == '%s')].active".formatted(tenantId)).isEqualTo(false);

		asMemberOf(REACTIVATE, Map.of(), alpha, tenantId)
				.expectBody()
				.jsonPath("$.errors").doesNotExist()
				.jsonPath("$.data.reactivateTenant.tenant.active").isEqualTo(true)
				.jsonPath("$.data.reactivateTenant.tenant.inactivatedAt").isEmpty();
		asMemberOf(CURRENT, Map.of(), alpha, tenantId)
				.expectBody()
				.jsonPath("$.data.currentTenant.name").isEqualTo("New Alpha Tenant");
	}

	@Test
	void myTenants_isScopedToTheCaller() {
		var alphaTenant = createTenantAs(alpha, "Scoped Alpha");
		graphqlAs(MINE, Map.of(), beta)
				.expectBody()
				.jsonPath("$.data.myTenants[?(@.id == '%s')]".formatted(alphaTenant)).isEmpty();
	}

	@Test
	void operations_withoutSession_areUnauthorized() {
		assertGraphQlError(graphql(MINE), "UNAUTHORIZED");
		assertGraphQlError(graphql(CREATE, input("Nope")), "UNAUTHORIZED");
	}

	@Test
	void currentTenant_returnsTheTenantToItsMember() {
		var alphaTenant = createTenantAs(alpha, "Private Alpha");
		asMemberOf(CURRENT, Map.of(), alpha, alphaTenant)
				.expectBody()
				.jsonPath("$.data.currentTenant.name").isEqualTo("Private Alpha");
	}

	@Test
	void inactiveTenant_rejectsEveryTenantOperationExceptReactivation() {
		var tenantId = createTenantAs(alpha, "Soon Inactive");
		asMemberOf(INACTIVATE, Map.of(), alpha, tenantId).expectBody().jsonPath("$.errors").doesNotExist();
		try {
			var denied = asMemberOf(CURRENT, Map.of(), alpha, tenantId);
			assertGraphQlError(denied, "FORBIDDEN");
			assertGraphQlError(asMemberOf(UPDATE, input("Nope"), alpha, tenantId), "FORBIDDEN");
			assertGraphQlError(asMemberOf(INACTIVATE, Map.of(), alpha, tenantId), "FORBIDDEN");
			asMemberOf(CURRENT, Map.of(), alpha, tenantId)
					.expectBody()
					.jsonPath("$.errors[0].message").isEqualTo("This tenant is inactive");
		} finally {
			asMemberOf(REACTIVATE, Map.of(), alpha, tenantId).expectBody().jsonPath("$.errors").doesNotExist();
		}
		asMemberOf(CURRENT, Map.of(), alpha, tenantId).expectBody().jsonPath("$.errors").doesNotExist();
	}

	/** The constraints themselves are covered by InputValidationTest; this proves the fetchers apply them. */
	@Test
	void createTenant_withAnInvalidName_isBadRequest() {
		assertGraphQlError(graphqlAs(CREATE, input(""), alpha), "BAD_REQUEST");
	}

	@Test
	void updateTenant_withAnInvalidName_isBadRequest() {
		var tenantId = createTenantAs(alpha, "To Rename");
		assertGraphQlError(asMemberOf(UPDATE, input(""), alpha, tenantId), "BAD_REQUEST");
	}
}
