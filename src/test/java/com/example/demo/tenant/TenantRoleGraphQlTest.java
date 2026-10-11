package com.example.demo.tenant;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.example.demo.BaseGraphQlTest;
import com.example.demo.TestUser;
import com.example.demo.tenant.adapter.outbound.persistence.TenantRoleRepository;
import com.example.demo.tenant.config.AccessCache;

class TenantRoleGraphQlTest extends BaseGraphQlTest {
	private static final String CREATE_ROLE = """
			mutation($input: CreateTenantRoleInput!) { createTenantRole(input: $input) { tenantRole { id name permissions { code } } } }""";
	private static final String UPDATE_ROLE = """
			mutation($input: UpdateTenantRoleInput!) { updateTenantRole(input: $input) { tenantRole { id name permissions { code } } } }""";
	private static final String INACTIVATE_ROLE = """
			mutation($input: TenantRoleIdInput!) { inactivateTenantRole(input: $input) { tenantRole { id active inactivatedAt } } }""";
	private static final String REACTIVATE_ROLE = """
			mutation($input: TenantRoleIdInput!) { reactivateTenantRole(input: $input) { tenantRole { id active inactivatedAt } } }""";
	private static final String ROLES = """
			query($first: Int, $after: String, $query: String) {
				tenantRoles(first: $first, after: $after, query: $query) {
					edges { cursor node { id name permissions { code } } }
					pageInfo { hasNextPage hasPreviousPage startCursor endCursor }
				}
			}""";
	private static final String ROLE = "query($id: UUID!) { tenantRole(id: $id) { id name active permissions { code } } }";
	private static final String PERMISSIONS = "query { permissions { code description level } }";
	private static final String ACTIVE_ROLES = """
			query($active: Boolean) { tenantRoles(first: 100, active: $active) { edges { node { id name active } } } }""";

	@Autowired
	private TenantRoleRepository tenantRoleRepository;
	@Autowired
	private AccessCache accessCache;

	private TestUser alpha;
	private TestUser beta;
	private UUID alphaTenant;
	private UUID betaTenant;

	@BeforeAll
	void setup() {
		alpha = createUser("Role Alpha", "gql.role.alpha@mail.com");
		beta = createUser("Role Beta", "gql.role.beta@mail.com");
		alphaTenant = newTenant(alpha, "Role Alpha Tenant");
		betaTenant = newTenant(beta, "Role Beta Tenant");
	}

	@Test
	void createUpdateInactivate_fullFlow() {
		var roleId = newRole(alphaTenant, alpha, "Reader", List.of("tenant_roles:read", "tenants:invite"));

		graphqlIn(alphaTenant, alpha, ROLE, variables("id", roleId))
				.expectBody()
				.jsonPath("$.errors").doesNotExist()
				.jsonPath("$.data.tenantRole.name").isEqualTo("Reader")
				.jsonPath("$.data.tenantRole.permissions[*].code")
				.value(codes -> assertThat(toStrings(codes)).containsExactly("tenant_roles:read", "tenants:invite"));

		graphqlIn(alphaTenant, alpha, UPDATE_ROLE,
				variables("input", variables("id", roleId, "name", "Auditor", "permissionCodes", List.of("tenants:update"))))
				.expectBody()
				.jsonPath("$.errors").doesNotExist()
				.jsonPath("$.data.updateTenantRole.tenantRole.name").isEqualTo("Auditor")
				.jsonPath("$.data.updateTenantRole.tenantRole.permissions[*].code")
				.value(codes -> assertThat(toStrings(codes)).containsExactly("tenants:update"));

		graphqlIn(alphaTenant, alpha, INACTIVATE_ROLE, variables("input", variables("id", roleId)))
				.expectBody()
				.jsonPath("$.errors").doesNotExist()
				.jsonPath("$.data.inactivateTenantRole.tenantRole.active").isEqualTo(false)
				.jsonPath("$.data.inactivateTenantRole.tenantRole.inactivatedAt").isNotEmpty();
		graphqlIn(alphaTenant, alpha, ACTIVE_ROLES, variables("active", false))
				.expectBody()
				.jsonPath("$.data.tenantRoles.edges[?(@.node.id == '%s')]".formatted(roleId)).isNotEmpty();
		graphqlIn(alphaTenant, alpha, ACTIVE_ROLES, variables("active", true))
				.expectBody()
				.jsonPath("$.data.tenantRoles.edges[?(@.node.id == '%s')]".formatted(roleId)).isEmpty();

		graphqlIn(alphaTenant, alpha, REACTIVATE_ROLE, variables("input", variables("id", roleId)))
				.expectBody()
				.jsonPath("$.errors").doesNotExist()
				.jsonPath("$.data.reactivateTenantRole.tenantRole.active").isEqualTo(true)
				.jsonPath("$.data.reactivateTenantRole.tenantRole.inactivatedAt").isEmpty();
	}

	@Test
	void tenantRoles_pagesForwardWithoutGapsOrDuplicates() {
		var tenant = newTenant(alpha, "Paged Tenant");
		var created = new ArrayList<UUID>();
		for (int i = 1; i <= 5; i++) {
			created.add(newRole(tenant, alpha, "Paged " + i, List.of("tenant_roles:read")));
		}

		var pages = collectPages("tenantRoles", "id",
				after -> graphqlIn(tenant, alpha, ROLES, variables("first", 2, "after", after)));
		var seen = pages.values();

		// Admin (created with the tenant) plus the five roles, oldest first.
		assertThat(seen).hasSize(6).doesNotHaveDuplicates();
		assertThat(seen.subList(1, 6)).containsExactlyElementsOf(created.stream().map(UUID::toString).toList());
		assertThat(pages.count()).isEqualTo(3);
	}

	@Test
	void tenantRoles_filtersByName() {
		var tenant = newTenant(alpha, "Filter Tenant");
		newRole(tenant, alpha, "Billing Manager", List.of());
		newRole(tenant, alpha, "Support", List.of());

		graphqlIn(tenant, alpha, ROLES, variables("query", "Billing"))
				.expectBody()
				.jsonPath("$.errors").doesNotExist()
				.jsonPath("$.data.tenantRoles.edges.length()").isEqualTo(1)
				.jsonPath("$.data.tenantRoles.edges[0].node.name").isEqualTo("Billing Manager")
				.jsonPath("$.data.tenantRoles.edges[0].node.permissions.length()").isEqualTo(0);
	}

	@Test
	void permissions_areBatchedRegardlessOfPageSize() {
		var tenant = newTenant(alpha, "Batch Tenant");
		for (int i = 1; i <= 6; i++) {
			newRole(tenant, alpha, "Batch " + i, List.of("tenant_roles:read", "tenants:update"));
		}
		assertStatementCountIndependentOfPageSize(
				() -> graphqlIn(tenant, alpha, ROLES, variables("first", 1))
						.expectBody().jsonPath("$.errors").doesNotExist(),
				() -> graphqlIn(tenant, alpha, ROLES, variables("first", 7))
						.expectBody().jsonPath("$.errors").doesNotExist()
						.jsonPath("$.data.tenantRoles.edges.length()").isEqualTo(7));
	}

	@Test
	void rolesOfOtherTenants_areInvisible() {
		var alphaRole = newRole(alphaTenant, alpha, "Alpha Only", List.of());

		assertGraphQlError(graphqlIn(betaTenant, beta, ROLE, variables("id", alphaRole)), "NOT_FOUND");
		assertGraphQlError(graphqlIn(betaTenant, beta, INACTIVATE_ROLE, variables("input", variables("id", alphaRole))),
				"NOT_FOUND");
		graphqlIn(betaTenant, beta, ROLES, variables("first", 100))
				.expectBody()
				.jsonPath("$.data.tenantRoles.edges[?(@.node.id == '%s')]".formatted(alphaRole)).isEmpty();
		graphqlIn(alphaTenant, alpha, ROLE, variables("id", alphaRole)).expectBody().jsonPath("$.errors").doesNotExist();
	}

	/** A member whose only role grants tenant_roles:read, in a tenant of their own. */
	private record Holder(UUID tenantId, UUID roleId, TestUser owner, TestUser member) {
	}

	private Holder holderOfReadRole(String suffix) {
		var owner = createUser("Owner " + suffix, "gql.role.owner." + suffix + "@mail.com");
		var member = createUser("Member " + suffix, "gql.role.member." + suffix + "@mail.com");
		var tenant = newTenant(owner, "Tenant " + suffix);
		var role = newRole(tenant, owner, "Reader " + suffix, List.of("tenant_roles:read"));
		addMember(tenant, member, role);
		return new Holder(tenant, role, owner, member);
	}

	@Test
	void deletedRole_grantsNoPermissions() {
		var holder = holderOfReadRole("deleted");

		graphqlIn(holder.tenantId(), holder.member(), ROLES, variables()).expectBody().jsonPath("$.errors").doesNotExist();

		tenantRoleRepository.deleteById(holder.roleId());
		// Deleting behind the services' back must be announced, or the cached grants stay.
		accessCache.invalidateRole(holder.tenantId(), holder.roleId());
		assertGraphQlError(graphqlIn(holder.tenantId(), holder.member(), ROLES, variables()), "FORBIDDEN");
	}

	@Test
	void roleMutations_withAnInvalidName_areBadRequest() {
		var roleId = newRole(alphaTenant, alpha, "Valid Name", List.of());

		assertGraphQlError(graphqlIn(alphaTenant, alpha, CREATE_ROLE,
				variables("input", variables("name", "", "permissionCodes", List.of()))), "BAD_REQUEST");
		assertGraphQlError(graphqlIn(alphaTenant, alpha, UPDATE_ROLE,
				variables("input", variables("id", roleId, "name", "", "permissionCodes", List.of()))), "BAD_REQUEST");
	}

	@Test
	void createTenantRole_withAnUnknownPermission_isBadRequest() {
		assertGraphQlError(graphqlIn(alphaTenant, alpha, CREATE_ROLE,
				variables("input", variables("name", "Bogus", "permissionCodes", List.of("not:a-permission")))),
				"BAD_REQUEST");
	}

	private static List<String> toStrings(Object ids) {
		return ((List<?>) ids).stream().map(Object::toString).toList();
	}

	@Test
	void theDefaultAdminRole_isBuiltInAndCannotBeChanged() {
		var tenant = newTenant(alpha, "Builtin Tenant");
		var adminId = new UUID[1];
		graphqlIn(tenant, alpha, "query { tenantRoles(first: 100) { edges { node { id name builtIn } } } }", variables())
				.expectBody()
				.jsonPath("$.errors").doesNotExist()
				.jsonPath("$.data.tenantRoles.edges[?(@.node.name == 'Admin')].node.builtIn")
				.value(v -> assertThat(toStrings(v)).containsExactly("true"))
				.jsonPath("$.data.tenantRoles.edges[?(@.node.name == 'Admin')].node.id")
				.value(v -> adminId[0] = UUID.fromString(toStrings(v).get(0)));
		var custom = newRole(tenant, alpha, "Custom", List.of("tenant_roles:read"));
		graphqlIn(tenant, alpha, "query($id: UUID!) { tenantRole(id: $id) { builtIn } }", variables("id", custom))
				.expectBody().jsonPath("$.data.tenantRole.builtIn").isEqualTo(false);

		assertGraphQlError(graphqlIn(tenant, alpha, UPDATE_ROLE,
				variables("input", variables("id", adminId[0], "name", "Renamed", "permissionCodes", List.of()))), "BAD_REQUEST");
		assertGraphQlError(graphqlIn(tenant, alpha, INACTIVATE_ROLE, variables("input", variables("id", adminId[0]))), "BAD_REQUEST");
		assertGraphQlError(graphqlIn(tenant, alpha, REACTIVATE_ROLE, variables("input", variables("id", adminId[0]))), "BAD_REQUEST");
		graphqlIn(tenant, alpha, ROLE, variables("id", adminId[0]))
				.expectBody().jsonPath("$.data.tenantRole.active").isEqualTo(true);
	}

	@Test
	void permissions_listsEveryPermissionWithItsLevel() {
		graphqlIn(alphaTenant, alpha, PERMISSIONS, variables())
				.expectBody()
				.jsonPath("$.errors").doesNotExist()
				.jsonPath("$.data.permissions[?(@.code == 'tenants:invite')].description").exists()
				.jsonPath("$.data.permissions[?(@.code == 'tenant_roles:read')].level")
				.value(v -> assertThat(toStrings(v)).containsExactly("VIEW"))
				.jsonPath("$.data.permissions[?(@.code == 'tenant_roles:create')].level")
				.value(v -> assertThat(toStrings(v)).containsExactly("EDIT"))
				.jsonPath("$.data.permissions[?(@.code == 'tenants:expel')].level")
				.value(v -> assertThat(toStrings(v)).containsExactly("SENSITIVE"));
	}
}
