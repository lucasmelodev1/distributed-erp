package com.example.demo.tenant;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.example.demo.BaseGraphQlTest;

/** {@code Tenant.permissions} tells the frontend what the caller may do in each of their tenants. */
class TenantPermissionsGraphQlTest extends BaseGraphQlTest {
	private static final String MY_PERMISSIONS = "query { myTenants { id permissions } }";


	@Test
	void member_seesTheUnionOfTheirRolesPermissionsSorted() {
		var owner = createUser("Perm Owner", "perm.owner@mail.com");
		var member = createUser("Perm Member", "perm.member@mail.com");
		var tenant = newTenant(owner, "Perm Tenant");
		var first = newRole(tenant, owner, "First", List.of("tenants:invite", "tenant_roles:read"));
		var second = newRole(tenant, owner, "Second", List.of("tenant_roles:read", "tenant_users:read"));
		addMemberWithRoles(tenant, member, List.of(first, second));

		graphqlAs(MY_PERMISSIONS, variables(), member)
				.expectBody()
				.jsonPath("$.errors").doesNotExist()
				.jsonPath("$.data.myTenants[0].permissions")
				.isEqualTo(List.of("tenant_roles:read", "tenant_users:read", "tenants:invite"));
	}

	@Test
	void inactiveMember_hasNoPermissions() {
		var owner = createUser("Perm Owner Two", "perm.owner2@mail.com");
		var member = createUser("Perm Member Two", "perm.member2@mail.com");
		var tenant = newTenant(owner, "Perm Tenant Two");
		var role = newRole(tenant, owner, "Reader", List.of("tenant_roles:read"));
		addMember(tenant, member, role);

		graphqlIn(tenant, owner, """
				mutation($input: TenantUserIdInput!) { inactivateTenantUser(input: $input) { tenantUser { active } } }""",
				variables("input", variables("userId", member.user().getId())))
				.expectBody().jsonPath("$.errors").doesNotExist();

		graphqlAs(MY_PERMISSIONS, variables(), member)
				.expectBody()
				.jsonPath("$.errors").doesNotExist()
				.jsonPath("$.data.myTenants[0].permissions").isEmpty();
	}

	@Test
	void anonymous_isUnauthorized() {
		assertGraphQlError(graphql(MY_PERMISSIONS), "UNAUTHORIZED");
	}
}
