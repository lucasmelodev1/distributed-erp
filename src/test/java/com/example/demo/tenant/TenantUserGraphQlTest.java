package com.example.demo.tenant;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.HashMap;
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
import com.example.demo.tenant.adapter.outbound.persistence.TenantUserRepository;
import com.example.demo.tenant.adapter.outbound.persistence.TenantUserRoleRepository;
import com.example.demo.tenant.domain.TenantUser;
import com.example.demo.tenant.domain.TenantUserId;
import com.example.demo.tenant.domain.authorization.TenantUserRole;
import com.example.demo.tenant.domain.authorization.TenantUserRoleId;

import jakarta.persistence.EntityManagerFactory;

class TenantUserGraphQlTest extends BaseGraphQlTest {
	private static final String USERS = """
			query($first: Int, $after: String, $query: String, $active: Boolean) {
				tenantUsers(first: $first, after: $after, query: $query, active: $active) {
					edges { node { user { id name email } roles { name permissions { code } } active inactivatedAt } }
					pageInfo { hasNextPage endCursor }
				}
			}""";
	private static final String USER = """
			query($userId: UUID!) { tenantUser(userId: $userId) { user { email } roles { name } active } }""";
	private static final String UPDATE = """
			mutation($input: UpdateTenantUserInput!) {
				updateTenantUser(input: $input) { tenantUser { user { email } roles { name } } }
			}""";
	private static final String INACTIVATE = """
			mutation($input: TenantUserIdInput!) {
				inactivateTenantUser(input: $input) { tenantUser { active inactivatedAt } }
			}""";
	private static final String REMOVE = """
			mutation($input: TenantUserIdInput!) { removeTenantUser(input: $input) { removedUserId } }""";
	private static final String REACTIVATE = """
			mutation($input: TenantUserIdInput!) {
				reactivateTenantUser(input: $input) { tenantUser { active inactivatedAt } }
			}""";

	@Autowired
	private TenantUserRepository tenantUserRepository;
	@Autowired
	private TenantUserRoleRepository tenantUserRoleRepository;
	@Autowired
	private EntityManagerFactory entityManagerFactory;

	private TestUser admin;
	private TestUser viewer;
	private TestUser ivan;
	private TestUser outsider;
	private UUID tenant;
	private UUID viewerRole;
	private UUID otherTenantRole;

	@BeforeAll
	void setup() {
		admin = createUser("Alice Admin", "gql.members.admin@mail.com");
		viewer = createUser("Victor Viewer", "gql.members.viewer@mail.com");
		ivan = createUser("Ivan Idle", "gql.members.ivan@mail.com");
		outsider = createUser("Olga Outsider", "gql.members.outsider@mail.com");

		tenant = createTenant(admin, "Members Tenant");
		viewerRole = createRole(tenant, admin, "Viewer", List.of("tenant_users:read"));
		addMember(viewer, viewerRole);
		addMember(ivan, null);

		var otherTenant = createTenant(outsider, "Outsider Tenant");
		otherTenantRole = createRole(otherTenant, outsider, "Foreign", List.of("tenants:update"));
	}

	private static Map<String, Object> vars(Object... keyValues) {
		var map = new HashMap<String, Object>();
		for (int i = 0; i < keyValues.length; i += 2) {
			map.put((String) keyValues[i], keyValues[i + 1]);
		}
		return map;
	}

	private UUID createTenant(TestUser user, String name) {
		var id = new UUID[1];
		graphqlAs("mutation($input: CreateTenantInput!) { createTenant(input: $input) { tenant { id } } }",
				vars("input", vars("name", name)), user)
				.expectBody()
				.jsonPath("$.errors").doesNotExist()
				.jsonPath("$.data.createTenant.tenant.id").value(v -> id[0] = UUID.fromString(v.toString()));
		return id[0];
	}

	private UUID createRole(UUID tenantId, TestUser user, String name, List<String> permissions) {
		var id = new UUID[1];
		in(tenantId, user, """
				mutation($input: CreateTenantRoleInput!) { createTenantRole(input: $input) { tenantRole { id } } }""",
				vars("input", vars("name", name, "permissionCodes", permissions)))
				.expectBody()
				.jsonPath("$.errors").doesNotExist()
				.jsonPath("$.data.createTenantRole.tenantRole.id").value(v -> id[0] = UUID.fromString(v.toString()));
		return id[0];
	}

	private void addMember(TestUser user, UUID roleId) {
		tenantUserRepository.save(new TenantUser(new TenantUserId(tenant, user.user().getId())));
		if (roleId != null) {
			tenantUserRoleRepository
					.save(new TenantUserRole(new TenantUserRoleId(user.user().getId(), tenant, roleId)));
		}
	}

	private RestTestClient.ResponseSpec in(UUID tenantId, TestUser user, String document, Map<String, Object> variables) {
		return graphql(document, variables, user.cookie().getValue(), tenantId.toString());
	}

	private List<String> emails(String variablesDocument, Map<String, Object> variables) {
		var emails = new ArrayList<String>();
		in(tenant, admin, variablesDocument, variables)
				.expectBody()
				.jsonPath("$.errors").doesNotExist()
				.jsonPath("$.data.tenantUsers.edges[*].node.user.email").value(v -> emails.addAll(strings(v)));
		return emails;
	}

	private static List<String> strings(Object values) {
		return ((List<?>) values).stream().map(Object::toString).toList();
	}

	@Test
	void tenantUsers_listsMembersWithProfilesAndRoles() {
		in(tenant, admin, USERS, vars())
				.expectBody()
				.jsonPath("$.errors").doesNotExist()
				.jsonPath("$.data.tenantUsers.edges.length()").isEqualTo(3)
				.jsonPath("$.data.tenantUsers.edges[0].node.user.email").isEqualTo("gql.members.admin@mail.com")
				.jsonPath("$.data.tenantUsers.edges[0].node.user.name").isEqualTo("Alice Admin")
				.jsonPath("$.data.tenantUsers.edges[0].node.roles[0].name").isEqualTo("Admin")
				.jsonPath("$.data.tenantUsers.edges[0].node.roles[0].permissions.length()").value(
						n -> assertThat(((Number) n).intValue()).isGreaterThan(10))
				.jsonPath("$.data.tenantUsers.edges[1].node.roles[0].name").isEqualTo("Viewer")
				.jsonPath("$.data.tenantUsers.edges[2].node.roles.length()").isEqualTo(0)
				.jsonPath("$.data.tenantUsers.edges[2].node.active").isEqualTo(true);
	}

	@Test
	void tenantUsers_pagesForwardInStableOrder() {
		var all = emails(USERS, vars());
		assertThat(all).containsExactly("gql.members.admin@mail.com", "gql.members.viewer@mail.com",
				"gql.members.ivan@mail.com");

		var seen = new ArrayList<String>();
		String after = null;
		boolean hasNext = true;
		int pages = 0;
		while (hasNext) {
			var page = new Object[2];
			var response = in(tenant, admin, USERS, vars("first", 2, "after", after)).expectBody();
			response.jsonPath("$.errors").doesNotExist()
					.jsonPath("$.data.tenantUsers.edges[*].node.user.email").value(v -> seen.addAll(strings(v)))
					.jsonPath("$.data.tenantUsers.pageInfo.hasNextPage").value(v -> page[0] = v)
					.jsonPath("$.data.tenantUsers.pageInfo.endCursor").value(v -> page[1] = v);
			hasNext = (Boolean) page[0];
			after = (String) page[1];
			pages++;
			assertThat(pages).isLessThan(5);
		}
		assertThat(seen).containsExactlyElementsOf(all);
		assertThat(pages).isEqualTo(2);
	}

	@Test
	void tenantUsers_filtersByNameIgnoringCase() {
		assertThat(emails(USERS, vars("query", "vICTor"))).containsExactly("gql.members.viewer@mail.com");
		assertThat(emails(USERS, vars("query", "nobody"))).isEmpty();
	}

	@Test
	void inactivateAndReactivate_updateActiveStatusAndFilter() {
		in(tenant, admin, INACTIVATE, vars("input", vars("userId", ivan.user().getId())))
				.expectBody()
				.jsonPath("$.errors").doesNotExist()
				.jsonPath("$.data.inactivateTenantUser.tenantUser.active").isEqualTo(false)
				.jsonPath("$.data.inactivateTenantUser.tenantUser.inactivatedAt").isNotEmpty();

		assertThat(emails(USERS, vars("active", false))).containsExactly("gql.members.ivan@mail.com");
		assertThat(emails(USERS, vars("active", true))).containsExactly("gql.members.admin@mail.com",
				"gql.members.viewer@mail.com");

		in(tenant, admin, REACTIVATE, vars("input", vars("userId", ivan.user().getId())))
				.expectBody()
				.jsonPath("$.errors").doesNotExist()
				.jsonPath("$.data.reactivateTenantUser.tenantUser.active").isEqualTo(true)
				.jsonPath("$.data.reactivateTenantUser.tenantUser.inactivatedAt").isEmpty();
		assertThat(emails(USERS, vars("active", false))).isEmpty();
	}

	@Test
	void tenantUser_returnsMemberOrNull() {
		in(tenant, admin, USER, vars("userId", viewer.user().getId()))
				.expectBody()
				.jsonPath("$.errors").doesNotExist()
				.jsonPath("$.data.tenantUser.user.email").isEqualTo("gql.members.viewer@mail.com")
				.jsonPath("$.data.tenantUser.roles[0].name").isEqualTo("Viewer");
		in(tenant, admin, USER, vars("userId", outsider.user().getId()))
				.expectBody()
				.jsonPath("$.errors").doesNotExist()
				.jsonPath("$.data.tenantUser").isEmpty();
	}

	@Test
	void updateTenantUser_replacesRoles() {
		in(tenant, admin, UPDATE, vars("input", vars("userId", ivan.user().getId(), "tenantRoleIds", List.of(viewerRole))))
				.expectBody()
				.jsonPath("$.errors").doesNotExist()
				.jsonPath("$.data.updateTenantUser.tenantUser.roles[0].name").isEqualTo("Viewer");
		in(tenant, admin, USER, vars("userId", ivan.user().getId()))
				.expectBody()
				.jsonPath("$.data.tenantUser.roles.length()").isEqualTo(1);

		in(tenant, admin, UPDATE, vars("input", vars("userId", ivan.user().getId(), "tenantRoleIds", List.of())))
				.expectBody()
				.jsonPath("$.errors").doesNotExist()
				.jsonPath("$.data.updateTenantUser.tenantUser.roles.length()").isEqualTo(0);
	}

	@Test
	void updateTenantUser_withRoleOfAnotherTenant_isBadRequest() {
		assertGraphQlError(in(tenant, admin, UPDATE,
				vars("input", vars("userId", ivan.user().getId(), "tenantRoleIds", List.of(otherTenantRole)))),
				"BAD_REQUEST");
		assertGraphQlError(in(tenant, admin, UPDATE,
				vars("input", vars("userId", ivan.user().getId(), "tenantRoleIds", List.of(UUID.randomUUID())))),
				"BAD_REQUEST");
	}

	@Test
	void inactivate_unknownMember_isNotFound() {
		assertGraphQlError(in(tenant, admin, INACTIVATE, vars("input", vars("userId", outsider.user().getId()))),
				"NOT_FOUND");
	}

	@Test
	void inactiveMember_isDeniedUntilReactivated() {
		in(tenant, viewer, USERS, vars()).expectBody().jsonPath("$.errors").doesNotExist();

		in(tenant, admin, INACTIVATE, vars("input", vars("userId", viewer.user().getId())))
				.expectBody().jsonPath("$.errors").doesNotExist();
		try {
			assertGraphQlError(in(tenant, viewer, USERS, vars()), "FORBIDDEN");
			in(tenant, viewer, USERS, vars())
					.expectBody()
					.jsonPath("$.errors[0].message").isEqualTo("Your membership in this tenant is inactive");
		} finally {
			in(tenant, admin, REACTIVATE, vars("input", vars("userId", viewer.user().getId())))
					.expectBody().jsonPath("$.errors").doesNotExist();
		}
		in(tenant, viewer, USERS, vars()).expectBody().jsonPath("$.errors").doesNotExist();
	}

	@Test
	void removeTenantUser_removesTheMemberAndTheirRoleAssignments() {
		var leaver = createUser("Soon Gone", "gql.members.gone@mail.com");
		addMember(leaver, viewerRole);
		assertThat(emails(USERS, vars())).contains("gql.members.gone@mail.com");

		in(tenant, admin, REMOVE, vars("input", vars("userId", leaver.user().getId())))
				.expectBody()
				.jsonPath("$.errors").doesNotExist()
				.jsonPath("$.data.removeTenantUser.removedUserId").isEqualTo(leaver.user().getId().toString());

		assertThat(emails(USERS, vars())).doesNotContain("gql.members.gone@mail.com");
		in(tenant, admin, USER, vars("userId", leaver.user().getId()))
				.expectBody().jsonPath("$.data.tenantUser").isEmpty();
		assertGraphQlError(in(tenant, leaver, USERS, vars()), "FORBIDDEN");
		assertThat(tenantUserRoleRepository
				.existsById(new TenantUserRoleId(leaver.user().getId(), tenant, viewerRole))).isFalse();
		// The user account itself is untouched.
		graphqlAs("{ me { email } }", vars(), leaver).expectBody()
				.jsonPath("$.data.me.email").isEqualTo("gql.members.gone@mail.com");

		assertGraphQlError(in(tenant, admin, REMOVE, vars("input", vars("userId", leaver.user().getId()))),
				"NOT_FOUND");
	}

	@Test
	void removeTenantUser_isGuardedAndPermissioned() {
		assertGraphQlError(in(tenant, admin, REMOVE, vars("input", vars("userId", admin.user().getId()))),
				"BAD_REQUEST");
		assertGraphQlError(in(tenant, admin, REMOVE, vars("input", vars("userId", outsider.user().getId()))),
				"NOT_FOUND");
		// The viewer can read members but lacks tenants:expel; the outsider is not a member at all.
		assertGraphQlError(in(tenant, viewer, REMOVE, vars("input", vars("userId", ivan.user().getId()))),
				"FORBIDDEN");
		assertGraphQlError(in(tenant, outsider, REMOVE, vars("input", vars("userId", ivan.user().getId()))),
				"FORBIDDEN");
		assertThat(emails(USERS, vars())).contains("gql.members.ivan@mail.com", "gql.members.admin@mail.com");
	}

	@Test
	void inactivatingYourself_isRejected() {
		assertGraphQlError(in(tenant, admin, INACTIVATE, vars("input", vars("userId", admin.user().getId()))),
				"BAD_REQUEST");
		in(tenant, admin, USERS, vars()).expectBody().jsonPath("$.errors").doesNotExist();
	}

	@Test
	void permissions_areEnforced() {
		// The viewer may read but not change anything.
		in(tenant, viewer, USERS, vars()).expectBody().jsonPath("$.errors").doesNotExist();
		assertGraphQlError(in(tenant, viewer, UPDATE,
				vars("input", vars("userId", ivan.user().getId(), "tenantRoleIds", List.of()))), "FORBIDDEN");
		assertGraphQlError(in(tenant, viewer, INACTIVATE, vars("input", vars("userId", ivan.user().getId()))),
				"FORBIDDEN");

		// Someone who is not a member cannot do anything.
		assertGraphQlError(in(tenant, outsider, USERS, vars()), "FORBIDDEN");
		assertGraphQlError(in(tenant, outsider, USER, vars("userId", admin.user().getId())), "FORBIDDEN");
	}

	@Test
	void nestedFields_areBatchedRegardlessOfPageSize() {
		var statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
		statistics.setStatisticsEnabled(true);
		try {
			in(tenant, admin, USERS, vars("first", 1)).expectBody().jsonPath("$.errors").doesNotExist();
			statistics.clear();
			in(tenant, admin, USERS, vars("first", 1)).expectBody().jsonPath("$.errors").doesNotExist();
			long onePerPage = statistics.getPrepareStatementCount();

			statistics.clear();
			in(tenant, admin, USERS, vars("first", 3))
					.expectBody()
					.jsonPath("$.errors").doesNotExist()
					.jsonPath("$.data.tenantUsers.edges.length()").isEqualTo(3);
			long threePerPage = statistics.getPrepareStatementCount();

			assertThat(onePerPage).isGreaterThan(0);
			assertThat(threePerPage).isEqualTo(onePerPage);
		} finally {
			statistics.setStatisticsEnabled(false);
		}
	}
}
