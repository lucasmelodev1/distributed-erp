package com.example.demo.tenant;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.hibernate.SessionFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.client.RestTestClient;

import com.example.demo.BaseGraphQlTest;
import com.example.demo.TestUser;
import com.example.demo.tenant.adapter.outbound.persistence.TenantRoleRepository;
import com.example.demo.tenant.adapter.outbound.persistence.TenantUserRepository;
import com.example.demo.tenant.adapter.outbound.persistence.TenantUserRoleRepository;
import com.example.demo.tenant.domain.TenantUser;
import com.example.demo.tenant.domain.TenantUserId;
import com.example.demo.tenant.domain.authorization.TenantUserRole;
import com.example.demo.tenant.domain.authorization.TenantUserRoleId;

import jakarta.persistence.EntityManagerFactory;

class TenantRoleGraphQlTest extends BaseGraphQlTest {
	private static final String CREATE_TENANT = """
			mutation($input: CreateTenantInput!) { createTenant(input: $input) { tenant { id } } }""";
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
	private static final String ACTIVE_ROLES = """
			query($active: Boolean) { tenantRoles(first: 100, active: $active) { edges { node { id name active } } } }""";

	@Autowired
	private TenantUserRepository tenantUserRepository;
	@Autowired
	private TenantUserRoleRepository tenantUserRoleRepository;
	@Autowired
	private EntityManagerFactory entityManagerFactory;
	@Autowired
	private TenantRoleRepository tenantRoleRepository;

	private TestUser alpha;
	private TestUser beta;
	private TestUser limited;
	private UUID alphaTenant;
	private UUID betaTenant;

	@BeforeAll
	void setup() {
		alpha = createUser("Role Alpha", "gql.role.alpha@mail.com");
		beta = createUser("Role Beta", "gql.role.beta@mail.com");
		limited = createUser("Role Limited", "gql.role.limited@mail.com");
		alphaTenant = createTenant(alpha, "Role Alpha Tenant");
		betaTenant = createTenant(beta, "Role Beta Tenant");
	}

	private UUID createTenant(TestUser user, String name) {
		var id = new UUID[1];
		graphqlAs(CREATE_TENANT, Map.of("input", Map.of("name", name)), user)
				.expectBody()
				.jsonPath("$.errors").doesNotExist()
				.jsonPath("$.data.createTenant.tenant.id").value(v -> id[0] = UUID.fromString(v.toString()));
		return id[0];
	}

	private RestTestClient.ResponseSpec in(UUID tenant, TestUser user, String document, Map<String, Object> variables) {
		return graphql(document, variables, user.cookie().getValue(), tenant.toString());
	}

	private UUID createRole(UUID tenant, TestUser user, String name, List<String> permissionCodes) {
		var id = new UUID[1];
		in(tenant, user, CREATE_ROLE, Map.of("input", Map.of("name", name, "permissionCodes", permissionCodes)))
				.expectBody()
				.jsonPath("$.errors").doesNotExist()
				.jsonPath("$.data.createTenantRole.tenantRole.name").isEqualTo(name)
				.jsonPath("$.data.createTenantRole.tenantRole.id").value(v -> id[0] = UUID.fromString(v.toString()));
		return id[0];
	}

	private static Map<String, Object> vars(Object... keyValues) {
		var map = new java.util.HashMap<String, Object>();
		for (int i = 0; i < keyValues.length; i += 2) {
			map.put((String) keyValues[i], keyValues[i + 1]);
		}
		return map;
	}

	@Test
	void createUpdateInactivate_fullFlow() {
		var roleId = createRole(alphaTenant, alpha, "Reader", List.of("tenant_roles:read", "tenants:invite"));

		in(alphaTenant, alpha, ROLE, vars("id", roleId))
				.expectBody()
				.jsonPath("$.errors").doesNotExist()
				.jsonPath("$.data.tenantRole.name").isEqualTo("Reader")
				.jsonPath("$.data.tenantRole.permissions[*].code")
				.value(codes -> assertThat(toStrings(codes)).containsExactly("tenant_roles:read", "tenants:invite"));

		in(alphaTenant, alpha, UPDATE_ROLE,
				vars("input", vars("id", roleId, "name", "Auditor", "permissionCodes", List.of("tenants:update"))))
				.expectBody()
				.jsonPath("$.errors").doesNotExist()
				.jsonPath("$.data.updateTenantRole.tenantRole.name").isEqualTo("Auditor")
				.jsonPath("$.data.updateTenantRole.tenantRole.permissions[*].code")
				.value(codes -> assertThat(toStrings(codes)).containsExactly("tenants:update"));

		in(alphaTenant, alpha, INACTIVATE_ROLE, vars("input", vars("id", roleId)))
				.expectBody()
				.jsonPath("$.errors").doesNotExist()
				.jsonPath("$.data.inactivateTenantRole.tenantRole.active").isEqualTo(false)
				.jsonPath("$.data.inactivateTenantRole.tenantRole.inactivatedAt").isNotEmpty();
		in(alphaTenant, alpha, ACTIVE_ROLES, vars("active", false))
				.expectBody()
				.jsonPath("$.data.tenantRoles.edges[?(@.node.id == '%s')]".formatted(roleId)).isNotEmpty();
		in(alphaTenant, alpha, ACTIVE_ROLES, vars("active", true))
				.expectBody()
				.jsonPath("$.data.tenantRoles.edges[?(@.node.id == '%s')]".formatted(roleId)).isEmpty();

		in(alphaTenant, alpha, REACTIVATE_ROLE, vars("input", vars("id", roleId)))
				.expectBody()
				.jsonPath("$.errors").doesNotExist()
				.jsonPath("$.data.reactivateTenantRole.tenantRole.active").isEqualTo(true)
				.jsonPath("$.data.reactivateTenantRole.tenantRole.inactivatedAt").isEmpty();
	}

	@Test
	void tenantRoles_pagesForwardWithoutGapsOrDuplicates() {
		var tenant = createTenant(alpha, "Paged Tenant");
		var created = new ArrayList<UUID>();
		for (int i = 1; i <= 5; i++) {
			created.add(createRole(tenant, alpha, "Paged " + i, List.of("tenant_roles:read")));
		}

		var seen = new ArrayList<String>();
		var pages = 0;
		String after = null;
		boolean hasNext = true;
		while (hasNext) {
			var variables = vars("first", 2, "after", after);
			var page = new Object[2];
			in(tenant, alpha, ROLES, variables)
					.expectBody()
					.jsonPath("$.errors").doesNotExist()
					.jsonPath("$.data.tenantRoles.edges[*].node.id").value(ids -> seen.addAll(toStrings(ids)))
					.jsonPath("$.data.tenantRoles.pageInfo.hasNextPage").value(v -> page[0] = v)
					.jsonPath("$.data.tenantRoles.pageInfo.endCursor").value(v -> page[1] = v);
			pages++;
			hasNext = (Boolean) page[0];
			after = (String) page[1];
			assertThat(pages).isLessThan(10);
		}

		// Admin (created with the tenant) plus the five roles, oldest first.
		assertThat(seen).hasSize(6).doesNotHaveDuplicates();
		assertThat(seen.subList(1, 6)).containsExactlyElementsOf(created.stream().map(UUID::toString).toList());
		assertThat(pages).isEqualTo(3);
	}

	@Test
	void tenantRoles_filtersByName() {
		var tenant = createTenant(alpha, "Filter Tenant");
		createRole(tenant, alpha, "Billing Manager", List.of());
		createRole(tenant, alpha, "Support", List.of());

		in(tenant, alpha, ROLES, vars("query", "Billing"))
				.expectBody()
				.jsonPath("$.errors").doesNotExist()
				.jsonPath("$.data.tenantRoles.edges.length()").isEqualTo(1)
				.jsonPath("$.data.tenantRoles.edges[0].node.name").isEqualTo("Billing Manager")
				.jsonPath("$.data.tenantRoles.edges[0].node.permissions.length()").isEqualTo(0);
	}

	@Test
	void tenantRoles_invalidPaging_isBadRequest() {
		assertGraphQlError(in(alphaTenant, alpha, ROLES, vars("first", 0)), "BAD_REQUEST");
		assertGraphQlError(in(alphaTenant, alpha, ROLES, vars("first", 101)), "BAD_REQUEST");
		assertGraphQlError(in(alphaTenant, alpha, ROLES, vars("after", "garbage")), "BAD_REQUEST");
	}

	@Test
	void permissions_areBatchedRegardlessOfPageSize() {
		var tenant = createTenant(alpha, "Batch Tenant");
		for (int i = 1; i <= 6; i++) {
			createRole(tenant, alpha, "Batch " + i, List.of("tenant_roles:read", "tenants:update"));
		}
		var statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
		statistics.setStatisticsEnabled(true);
		try {
			in(tenant, alpha, ROLES, vars("first", 1)).expectBody().jsonPath("$.errors").doesNotExist();
			statistics.clear();
			in(tenant, alpha, ROLES, vars("first", 1)).expectBody().jsonPath("$.errors").doesNotExist();
			long onePerPage = statistics.getPrepareStatementCount();

			statistics.clear();
			in(tenant, alpha, ROLES, vars("first", 7))
					.expectBody()
					.jsonPath("$.errors").doesNotExist()
					.jsonPath("$.data.tenantRoles.edges.length()").isEqualTo(7);
			long sevenPerPage = statistics.getPrepareStatementCount();

			assertThat(onePerPage).isGreaterThan(0);
			assertThat(sevenPerPage).isEqualTo(onePerPage);
		} finally {
			statistics.setStatisticsEnabled(false);
		}
	}

	@Test
	void rolesOfOtherTenants_areInvisible() {
		var alphaRole = createRole(alphaTenant, alpha, "Alpha Only", List.of());

		assertGraphQlError(in(alphaTenant, beta, ROLES, vars()), "FORBIDDEN");
		assertGraphQlError(in(betaTenant, beta, ROLE, vars("id", alphaRole)), "NOT_FOUND");
		assertGraphQlError(in(betaTenant, beta, INACTIVATE_ROLE, vars("input", vars("id", alphaRole))),
				"NOT_FOUND");
		in(betaTenant, beta, ROLES, vars("first", 100))
				.expectBody()
				.jsonPath("$.data.tenantRoles.edges[?(@.node.id == '%s')]".formatted(alphaRole)).isEmpty();
		in(alphaTenant, alpha, ROLE, vars("id", alphaRole)).expectBody().jsonPath("$.errors").doesNotExist();
	}

	@Test
	void member_withoutPermission_isForbidden() {
		var emptyRole = createRole(alphaTenant, alpha, "Nothing", List.of());
		tenantUserRepository.save(new TenantUser(new TenantUserId(alphaTenant, limited.user().getId())));
		tenantUserRoleRepository.save(new TenantUserRole(
				new TenantUserRoleId(limited.user().getId(), alphaTenant, emptyRole)));

		assertGraphQlError(in(alphaTenant, limited, ROLES, vars()), "FORBIDDEN");
		assertGraphQlError(in(alphaTenant, limited, CREATE_ROLE,
				vars("input", vars("name", "Sneaky", "permissionCodes", List.of()))), "FORBIDDEN");
	}

	/** A member whose only role grants tenant_roles:read, in a tenant of their own. */
	private record Holder(UUID tenantId, UUID roleId, TestUser owner, TestUser member) {
	}

	private Holder holderOfReadRole(String suffix) {
		var owner = createUser("Owner " + suffix, "gql.role.owner." + suffix + "@mail.com");
		var member = createUser("Member " + suffix, "gql.role.member." + suffix + "@mail.com");
		var tenant = createTenant(owner, "Tenant " + suffix);
		var role = createRole(tenant, owner, "Reader " + suffix, List.of("tenant_roles:read"));
		tenantUserRepository.save(new TenantUser(new TenantUserId(tenant, member.user().getId())));
		tenantUserRoleRepository.save(new TenantUserRole(new TenantUserRoleId(member.user().getId(), tenant, role)));
		return new Holder(tenant, role, owner, member);
	}

	@Test
	void inactiveRole_grantsNoPermissionsUntilReactivated() {
		var holder = holderOfReadRole("inactive");

		in(holder.tenantId(), holder.member(), ROLES, vars()).expectBody().jsonPath("$.errors").doesNotExist();

		in(holder.tenantId(), holder.owner(), INACTIVATE_ROLE, vars("input", vars("id", holder.roleId())))
				.expectBody().jsonPath("$.errors").doesNotExist();
		assertGraphQlError(in(holder.tenantId(), holder.member(), ROLES, vars()), "FORBIDDEN");

		in(holder.tenantId(), holder.owner(), REACTIVATE_ROLE, vars("input", vars("id", holder.roleId())))
				.expectBody().jsonPath("$.errors").doesNotExist();
		in(holder.tenantId(), holder.member(), ROLES, vars()).expectBody().jsonPath("$.errors").doesNotExist();
	}

	@Test
	void deletedRole_grantsNoPermissions() {
		var holder = holderOfReadRole("deleted");

		in(holder.tenantId(), holder.member(), ROLES, vars()).expectBody().jsonPath("$.errors").doesNotExist();

		tenantRoleRepository.deleteById(holder.roleId());
		assertGraphQlError(in(holder.tenantId(), holder.member(), ROLES, vars()), "FORBIDDEN");
	}

	@Test
	void invalidInput_isBadRequest() {
		assertGraphQlError(in(alphaTenant, alpha, CREATE_ROLE,
				vars("input", vars("name", "", "permissionCodes", List.of()))), "BAD_REQUEST");
		assertGraphQlError(in(alphaTenant, alpha, CREATE_ROLE,
				vars("input", vars("name", "x".repeat(256), "permissionCodes", List.of()))), "BAD_REQUEST");
		assertGraphQlError(in(alphaTenant, alpha, CREATE_ROLE,
				vars("input", vars("name", "Bogus", "permissionCodes", List.of("not:a-permission")))), "BAD_REQUEST");
	}

	private static List<String> toStrings(Object ids) {
		return ((List<?>) ids).stream().map(Object::toString).toList();
	}
}
