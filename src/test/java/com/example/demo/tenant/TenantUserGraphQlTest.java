package com.example.demo.tenant;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.example.demo.BaseGraphQlTest;
import com.example.demo.TestUser;
import com.example.demo.tenant.adapter.outbound.persistence.TenantUserRoleRepository;
import com.example.demo.tenant.domain.authorization.TenantUserRoleId;

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
	private TenantUserRoleRepository tenantUserRoleRepository;

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

		tenant = newTenant(admin, "Members Tenant");
		viewerRole = newRole(tenant, admin, "Viewer", List.of("tenant_users:read"));
		addMember(tenant, viewer, viewerRole);
		addMember(tenant, ivan, null);

		var otherTenant = newTenant(outsider, "Outsider Tenant");
		otherTenantRole = newRole(otherTenant, outsider, "Foreign", List.of("tenants:update"));
	}

	private List<String> emails(String variablesDocument, Map<String, Object> variables) {
		var emails = new ArrayList<String>();
		graphqlIn(tenant, admin, variablesDocument, variables)
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
		graphqlIn(tenant, admin, USERS, variables())
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
		var all = emails(USERS, variables());
		assertThat(all).containsExactly("gql.members.admin@mail.com", "gql.members.viewer@mail.com",
				"gql.members.ivan@mail.com");

		var pages = collectPages("tenantUsers", "user.email",
				after -> graphqlIn(tenant, admin, USERS, variables("first", 2, "after", after)));
		assertThat(pages.values()).containsExactlyElementsOf(all);
		assertThat(pages.count()).isEqualTo(2);
	}

	@Test
	void tenantUsers_filtersByNameIgnoringCase() {
		assertThat(emails(USERS, variables("query", "vICTor"))).containsExactly("gql.members.viewer@mail.com");
		assertThat(emails(USERS, variables("query", "nobody"))).isEmpty();
	}

	@Test
	void inactivateAndReactivate_updateActiveStatusAndFilter() {
		graphqlIn(tenant, admin, INACTIVATE, variables("input", variables("userId", ivan.user().getId())))
				.expectBody()
				.jsonPath("$.errors").doesNotExist()
				.jsonPath("$.data.inactivateTenantUser.tenantUser.active").isEqualTo(false)
				.jsonPath("$.data.inactivateTenantUser.tenantUser.inactivatedAt").isNotEmpty();

		assertThat(emails(USERS, variables("active", false))).containsExactly("gql.members.ivan@mail.com");
		assertThat(emails(USERS, variables("active", true))).containsExactly("gql.members.admin@mail.com",
				"gql.members.viewer@mail.com");

		graphqlIn(tenant, admin, REACTIVATE, variables("input", variables("userId", ivan.user().getId())))
				.expectBody()
				.jsonPath("$.errors").doesNotExist()
				.jsonPath("$.data.reactivateTenantUser.tenantUser.active").isEqualTo(true)
				.jsonPath("$.data.reactivateTenantUser.tenantUser.inactivatedAt").isEmpty();
		assertThat(emails(USERS, variables("active", false))).isEmpty();
	}

	@Test
	void tenantUser_returnsMemberOrNull() {
		graphqlIn(tenant, admin, USER, variables("userId", viewer.user().getId()))
				.expectBody()
				.jsonPath("$.errors").doesNotExist()
				.jsonPath("$.data.tenantUser.user.email").isEqualTo("gql.members.viewer@mail.com")
				.jsonPath("$.data.tenantUser.roles[0].name").isEqualTo("Viewer");
		graphqlIn(tenant, admin, USER, variables("userId", outsider.user().getId()))
				.expectBody()
				.jsonPath("$.errors").doesNotExist()
				.jsonPath("$.data.tenantUser").isEmpty();
	}

	@Test
	void updateTenantUser_replacesRoles() {
		graphqlIn(tenant, admin, UPDATE, variables("input", variables("userId", ivan.user().getId(), "tenantRoleIds", List.of(viewerRole))))
				.expectBody()
				.jsonPath("$.errors").doesNotExist()
				.jsonPath("$.data.updateTenantUser.tenantUser.roles[0].name").isEqualTo("Viewer");
		graphqlIn(tenant, admin, USER, variables("userId", ivan.user().getId()))
				.expectBody()
				.jsonPath("$.data.tenantUser.roles.length()").isEqualTo(1);

		graphqlIn(tenant, admin, UPDATE, variables("input", variables("userId", ivan.user().getId(), "tenantRoleIds", List.of())))
				.expectBody()
				.jsonPath("$.errors").doesNotExist()
				.jsonPath("$.data.updateTenantUser.tenantUser.roles.length()").isEqualTo(0);
	}

	@Test
	void updateTenantUser_withRoleOfAnotherTenant_isBadRequest() {
		assertGraphQlError(graphqlIn(tenant, admin, UPDATE,
				variables("input", variables("userId", ivan.user().getId(), "tenantRoleIds", List.of(otherTenantRole)))),
				"BAD_REQUEST");
		assertGraphQlError(graphqlIn(tenant, admin, UPDATE,
				variables("input", variables("userId", ivan.user().getId(), "tenantRoleIds", List.of(UUID.randomUUID())))),
				"BAD_REQUEST");
	}

	@Test
	void inactivate_unknownMember_isNotFound() {
		assertGraphQlError(graphqlIn(tenant, admin, INACTIVATE, variables("input", variables("userId", outsider.user().getId()))),
				"NOT_FOUND");
	}

	@Test
	void removeTenantUser_removesTheMemberAndTheirRoleAssignments() {
		var leaver = createUser("Soon Gone", "gql.members.gone@mail.com");
		addMember(tenant, leaver, viewerRole);
		assertThat(emails(USERS, variables())).contains("gql.members.gone@mail.com");

		graphqlIn(tenant, admin, REMOVE, variables("input", variables("userId", leaver.user().getId())))
				.expectBody()
				.jsonPath("$.errors").doesNotExist()
				.jsonPath("$.data.removeTenantUser.removedUserId").isEqualTo(leaver.user().getId().toString());

		assertThat(emails(USERS, variables())).doesNotContain("gql.members.gone@mail.com");
		graphqlIn(tenant, admin, USER, variables("userId", leaver.user().getId()))
				.expectBody().jsonPath("$.data.tenantUser").isEmpty();
		assertThat(tenantUserRoleRepository
				.existsById(new TenantUserRoleId(leaver.user().getId(), tenant, viewerRole))).isFalse();
		// The user account itself is untouched.
		graphqlAs("{ me { email } }", variables(), leaver).expectBody()
				.jsonPath("$.data.me.email").isEqualTo("gql.members.gone@mail.com");

		assertGraphQlError(graphqlIn(tenant, admin, REMOVE, variables("input", variables("userId", leaver.user().getId()))),
				"NOT_FOUND");
	}

	@Test
	void removeTenantUser_isGuarded() {
		assertGraphQlError(graphqlIn(tenant, admin, REMOVE, variables("input", variables("userId", admin.user().getId()))),
				"BAD_REQUEST");
		assertGraphQlError(graphqlIn(tenant, admin, REMOVE, variables("input", variables("userId", outsider.user().getId()))),
				"NOT_FOUND");
		assertThat(emails(USERS, variables())).contains("gql.members.ivan@mail.com", "gql.members.admin@mail.com");
	}

	@Test
	void inactivatingYourself_isRejected() {
		assertGraphQlError(graphqlIn(tenant, admin, INACTIVATE, variables("input", variables("userId", admin.user().getId()))),
				"BAD_REQUEST");
		graphqlIn(tenant, admin, USERS, variables()).expectBody().jsonPath("$.errors").doesNotExist();
	}

	@Test
	void nestedFields_areBatchedRegardlessOfPageSize() {
		assertStatementCountIndependentOfPageSize(
				() -> graphqlIn(tenant, admin, USERS, variables("first", 1))
						.expectBody().jsonPath("$.errors").doesNotExist(),
				() -> graphqlIn(tenant, admin, USERS, variables("first", 3))
						.expectBody().jsonPath("$.errors").doesNotExist()
						.jsonPath("$.data.tenantUsers.edges.length()").isEqualTo(3));
	}

	@Test
	void updateTenantUser_cannotAddAnInactiveRole_butMayKeepOne() {
		var retired = newRole(tenant, admin, "Retired Role", List.of("tenant_users:read"));
		graphqlIn(tenant, admin, UPDATE, variables("input", variables("userId", ivan.user().getId(), "tenantRoleIds", List.of(retired))))
				.expectBody().jsonPath("$.errors").doesNotExist();
		graphqlIn(tenant, admin, "mutation($input: TenantRoleIdInput!) { inactivateTenantRole(input: $input) { tenantRole { id } } }",
				variables("input", variables("id", retired))).expectBody().jsonPath("$.errors").doesNotExist();

		// Keeping the inactive role they already hold is allowed; adding it to another member is not.
		graphqlIn(tenant, admin, UPDATE,
				variables("input", variables("userId", ivan.user().getId(), "tenantRoleIds", List.of(retired, viewerRole))))
				.expectBody().jsonPath("$.errors").doesNotExist();
		assertGraphQlError(graphqlIn(tenant, admin, UPDATE,
				variables("input", variables("userId", viewer.user().getId(), "tenantRoleIds", List.of(retired)))), "BAD_REQUEST");
	}

	@Test
	void updateTenantUser_cannotChangeOwnRoles_orGrantMorePermissionsThanHeld() {
		assertGraphQlError(graphqlIn(tenant, admin, UPDATE,
				variables("input", variables("userId", admin.user().getId(), "tenantRoleIds", List.of(viewerRole)))), "BAD_REQUEST");

		// Own tenant, so the extra members do not show up in the other tests' member lists.
		var manager = createUser("Role Manager", "gql.members.manager@mail.com");
		var target = createUser("Role Target", "gql.members.target@mail.com");
		var own = newTenant(manager, "Role Manager Tenant");
		var managerRole = newRole(own, manager, "Role manager", List.of("tenant_users:update", "tenant_roles:read"));
		var strong = newRole(own, manager, "Strong", List.of("tenant_users:update", "tenant_roles:read", "tenants:expel"));
		var weak = newRole(own, manager, "Weak", List.of("tenant_roles:read"));
		var limitedManager = createUser("Limited Manager", "gql.members.limited@mail.com");
		addMember(own, target, null);
		addMember(own, limitedManager, managerRole);

		// The creator's Admin role holds everything, so they may hand out any role.
		graphqlIn(own, manager, UPDATE, variables("input", variables("userId", target.user().getId(), "tenantRoleIds", List.of(strong))))
				.expectBody().jsonPath("$.errors").doesNotExist();
		// A member who holds only tenant_users:update + tenant_roles:read cannot hand out a role with tenants:expel.
		graphqlIn(own, limitedManager, UPDATE, variables("input", variables("userId", target.user().getId(), "tenantRoleIds", List.of(weak))))
				.expectBody().jsonPath("$.errors").doesNotExist();
		assertGraphQlError(graphqlIn(own, limitedManager, UPDATE,
				variables("input", variables("userId", target.user().getId(), "tenantRoleIds", List.of(strong)))), "FORBIDDEN");
	}
}
