package com.example.demo.tenant;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import com.example.demo.BaseGraphQlTest;
import com.example.demo.TestUser;
import com.example.demo.tenant.adapter.outbound.persistence.TenantRoleRepository;

class TenantInvitationGraphQlTest extends BaseGraphQlTest {
	private static final Pattern CODE = Pattern.compile("accept-invitation\\?code=([0-9a-fA-F-]{36})");

	private static final String INVITE = """
			mutation($input: InviteUserToTenantInput!) {
				inviteUserToTenant(input: $input) { invitation { id email status expiresAt roles { name } } }
			}""";
	private static final String REVOKE = """
			mutation($input: RevokeInvitationInput!) {
				revokeInvitation(input: $input) { invitation { id status } }
			}""";
	private static final String ACCEPT = """
			mutation($input: AcceptTenantInvitationInput!) { acceptTenantInvitation(input: $input) { tenant { id name } } }""";
	private static final String REGISTER = """
			mutation($input: RegisterUserFromInvitationInput!) {
				registerUserFromInvitation(input: $input) { id email role }
			}""";
	private static final String CHECK = """
			query($code: String!) { checkInvitedUser(code: $code) { nextAction email tenantName } }""";
	private static final String INVITATIONS = """
			query($first: Int, $after: String, $status: TenantInvitationStatus) {
				tenantInvitations(first: $first, after: $after, status: $status) {
					edges { node { id email status roles { name } } }
					pageInfo { hasNextPage endCursor }
				}
			}""";
	private static final String MEMBERS = """
			query { tenantUsers(first: 100) { edges { node { user { email } roles { name } active } } } }""";

	@Autowired
	private TenantRoleRepository tenantRoleRepository;
	@Autowired
	private JdbcTemplate jdbcTemplate;

	private TestUser admin;
	private TestUser outsider;
	private UUID tenant;
	private UUID memberRole;
	private UUID otherTenantRole;

	@BeforeAll
	void setup() {
		admin = createUser("Invite Admin", "gql.inv.admin@mail.com");
		outsider = createUser("Invite Outsider", "gql.inv.outsider@mail.com");
		tenant = newTenant(admin, "Invite Tenant");
		memberRole = newRole(tenant, admin, "Member", List.of("tenant_roles:read"));
		var outsiderTenant = newTenant(outsider, "Outsider Tenant");
		otherTenantRole = newRole(outsiderTenant, outsider, "Foreign", List.of());
	}

	// ---- helpers

	private java.util.Map<String, Object> inviteVars(String email, UUID... roleIds) {
		return variables("input", variables("email", email, "tenantRoleIds", List.of(roleIds)));
	}

	/** Invites the address and returns the invitation id and the code from the email it was sent. */
	private record Issued(UUID id, String code) {
	}

	private Issued issue(String email, UUID... roleIds) {
		var id = new UUID[1];
		graphqlIn(tenant, admin, INVITE, inviteVars(email, roleIds))
				.expectBody()
				.jsonPath("$.errors").doesNotExist()
				.jsonPath("$.data.inviteUserToTenant.invitation.id")
				.value(v -> id[0] = UUID.fromString(v.toString()));
		return new Issued(id[0], codeSentTo(email));
	}

	private String codeSentTo(String email) {
		// An earlier email to the same address may already be recorded; wait for the newest one.
		awaitEmailsProcessed();
		var matcher = CODE.matcher(sentEmails.lastTextTo(email));
		assertThat(matcher.find()).as("invitation link in the email").isTrue();
		return matcher.group(1);
	}

	private java.util.Map<String, Object> codeVars(String code) {
		return variables("code", code);
	}

	private java.util.Map<String, Object> codeInput(String code) {
		return variables("input", variables("code", code));
	}

	private void expireNow(UUID invitationId) {
		jdbcTemplate.update("UPDATE tenant_invitations SET expires_at = ? WHERE id = UNHEX(REPLACE(?, '-', ''))",
				Timestamp.from(Instant.now().minus(1, ChronoUnit.DAYS)), invitationId.toString());
	}

	private List<String> memberEmails(UUID tenantId, TestUser user) {
		var emails = new ArrayList<String>();
		graphqlIn(tenantId, user, MEMBERS, variables())
				.expectBody()
				.jsonPath("$.errors").doesNotExist()
				.jsonPath("$.data.tenantUsers.edges[*].node.user.email")
				.value(v -> ((List<?>) v).forEach(e -> emails.add(e.toString())));
		return emails;
	}

	// ---- issuing

	@Test
	void invite_emailsTheCodeAndNeverReturnsIt() {
		var body = graphqlIn(tenant, admin, INVITE, inviteVars("Gql.Inv.One@Mail.com", memberRole))
				.expectStatus().isOk()
				.expectBody(String.class)
				.returnResult().getResponseBody();

		var code = codeSentTo("gql.inv.one@mail.com");
		assertThat(body).doesNotContain(code);
		assertThat(body).contains("\"email\":\"gql.inv.one@mail.com\"", "\"status\":\"PENDING\"", "Member");
		assertThat(sentEmails.lastTo("gql.inv.one@mail.com").orElseThrow().subject()).contains("Invite Tenant");

		graphqlIn(tenant, admin, INVITATIONS, variables("status", "PENDING"))
				.expectBody()
				.jsonPath("$.errors").doesNotExist()
				.jsonPath("$.data.tenantInvitations.edges[?(@.node.email == 'gql.inv.one@mail.com')].node.roles[0].name")
				.isEqualTo("Member");
	}

	@Test
	void invite_withAnInvalidEmailOrNoRoles_isBadRequest() {
		assertGraphQlError(graphqlIn(tenant, admin, INVITE, inviteVars("not-an-email", memberRole)), "BAD_REQUEST");
		assertGraphQlError(graphqlIn(tenant, admin, INVITE, inviteVars("gql.inv.norole@mail.com")), "BAD_REQUEST");
	}

	@Test
	void invite_isRejectedForInvalidRequests() {
		assertGraphQlError(graphqlIn(tenant, admin, INVITE, inviteVars("gql.inv.x@mail.com", otherTenantRole)),
				"BAD_REQUEST");
		assertGraphQlError(graphqlIn(tenant, admin, INVITE, inviteVars("gql.inv.x@mail.com", UUID.randomUUID())),
				"BAD_REQUEST");
		assertGraphQlError(graphqlIn(tenant, admin, INVITE, inviteVars("gql.inv.admin@mail.com", memberRole)),
				"BAD_REQUEST");
	}

	@Test
	void invitingAgain_revokesThePreviousPendingInvitation() {
		var first = issue("gql.inv.again@mail.com", memberRole);
		var second = issue("gql.inv.again@mail.com", memberRole);

		assertThat(second.code()).isNotEqualTo(first.code());
		assertGraphQlError(graphql(CHECK, codeVars(first.code())), "NOT_FOUND");
		graphql(CHECK, codeVars(second.code())).expectBody().jsonPath("$.errors").doesNotExist();
		graphqlIn(tenant, admin, INVITATIONS, variables("status", "REVOKED"))
				.expectBody()
				.jsonPath("$.data.tenantInvitations.edges[?(@.node.id == '%s')]".formatted(first.id())).isNotEmpty();
	}

	// ---- checking and accepting

	@Test
	void existingUser_checksThenAccepts_andJoinsWithTheInvitedRoles() {
		var invitee = createUser("Existing Invitee", "gql.inv.existing@mail.com");
		var stranger = createUser("Stranger", "gql.inv.stranger@mail.com");
		var issued = issue("gql.inv.existing@mail.com", memberRole);

		graphql(CHECK, codeVars(issued.code()))
				.expectBody()
				.jsonPath("$.errors").doesNotExist()
				.jsonPath("$.data.checkInvitedUser.nextAction").isEqualTo("LOGIN")
				.jsonPath("$.data.checkInvitedUser.email").isEqualTo("gql.inv.existing@mail.com")
				.jsonPath("$.data.checkInvitedUser.tenantName").isEqualTo("Invite Tenant");
		graphqlAs(CHECK, codeVars(issued.code()), invitee)
				.expectBody().jsonPath("$.data.checkInvitedUser.nextAction").isEqualTo("ACCEPT");
		graphqlAs(CHECK, codeVars(issued.code()), stranger)
				.expectBody().jsonPath("$.data.checkInvitedUser.nextAction").isEqualTo("WRONG_ACCOUNT");

		assertGraphQlError(graphqlAs(ACCEPT, codeInput(issued.code()), stranger), "FORBIDDEN");
		assertGraphQlError(graphql(ACCEPT, codeInput(issued.code())), "UNAUTHORIZED");

		graphqlAs(ACCEPT, codeInput(issued.code()), invitee)
				.expectBody()
				.jsonPath("$.errors").doesNotExist()
				.jsonPath("$.data.acceptTenantInvitation.tenant.id").isEqualTo(tenant.toString());

		graphqlIn(tenant, admin, MEMBERS, variables())
				.expectBody()
				.jsonPath("$.data.tenantUsers.edges[?(@.node.user.email == 'gql.inv.existing@mail.com')].node.roles[0].name")
				.isEqualTo("Member");
		graphqlIn(tenant, invitee, "{ tenantRoles(first: 1) { edges { node { id } } } }", variables())
				.expectBody().jsonPath("$.errors").doesNotExist();

		graphqlIn(tenant, admin, INVITATIONS, variables("status", "ACCEPTED"))
				.expectBody()
				.jsonPath("$.data.tenantInvitations.edges[?(@.node.id == '%s')]".formatted(issued.id())).isNotEmpty();
		assertGraphQlError(graphqlAs(ACCEPT, codeInput(issued.code()), invitee), "NOT_FOUND");
		assertGraphQlError(graphql(CHECK, codeVars(issued.code())), "NOT_FOUND");
	}

	@Test
	void newUser_registersFromTheInvitation_andIsLoggedIn() {
		var email = "gql.inv.newcomer@mail.com";
		var issued = issue(email, memberRole);

		graphql(CHECK, codeVars(issued.code()))
				.expectBody().jsonPath("$.data.checkInvitedUser.nextAction").isEqualTo("CREATE_ACCOUNT");

		assertGraphQlError(graphql(REGISTER, variables("input",
				variables("code", issued.code(), "name", "New Comer", "password", "short"))), "BAD_REQUEST");
		assertGraphQlError(graphql(REGISTER, variables("input",
				variables("code", UUID.randomUUID().toString(), "name", "New Comer", "password", "Password@123"))),
				"NOT_FOUND");

		var session = graphql(REGISTER, variables("input",
				variables("code", issued.code(), "name", "New Comer", "password", "Password@123")))
				.expectStatus().isOk()
				.expectCookie().exists(SESSION_NAME)
				.expectBody()
				.jsonPath("$.errors").doesNotExist()
				.jsonPath("$.data.registerUserFromInvitation.email").isEqualTo(email)
				.returnResult()
				.getResponseCookies().getFirst(SESSION_NAME).getValue();

		graphql("{ me { email name } }", variables(), session, null)
				.expectBody().jsonPath("$.data.me.name").isEqualTo("New Comer");
		graphql("{ tenantRoles(first: 1) { edges { node { id } } } }", variables(), session, tenant.toString())
				.expectBody().jsonPath("$.errors").doesNotExist();
		assertThat(memberEmails(tenant, admin)).contains(email);

		assertGraphQlError(graphql(REGISTER, variables("input",
				variables("code", issued.code(), "name", "Again", "password", "Password@123"))), "NOT_FOUND");
	}

	@Test
	void registering_whenAnAccountAlreadyExists_isRejected() {
		createUser("Has Account", "gql.inv.hasaccount@mail.com");
		var issued = issue("gql.inv.hasaccount@mail.com", memberRole);

		assertGraphQlError(graphql(REGISTER, variables("input",
				variables("code", issued.code(), "name", "Has Account", "password", "Password@123"))), "BAD_REQUEST");
		graphql(CHECK, codeVars(issued.code()))
				.expectBody().jsonPath("$.data.checkInvitedUser.nextAction").isEqualTo("LOGIN");
	}

	@Test
	void accepting_skipsRolesDeletedSinceTheInvitation() {
		var invitee = createUser("Skips Roles", "gql.inv.skips@mail.com");
		var doomed = newRole(tenant, admin, "Doomed", List.of());
		var issued = issue("gql.inv.skips@mail.com", memberRole, doomed);

		tenantRoleRepository.deleteById(doomed);
		graphqlAs(ACCEPT, codeInput(issued.code()), invitee).expectBody().jsonPath("$.errors").doesNotExist();

		graphqlIn(tenant, admin, MEMBERS, variables())
				.expectBody()
				.jsonPath("$.data.tenantUsers.edges[?(@.node.user.email == 'gql.inv.skips@mail.com')].node.roles.length()")
				.isEqualTo(1);
	}

	@Test
	void accepting_whileAlreadyAMember_isRejected() {
		var invitee = createUser("Already Active", "gql.inv.active@mail.com");
		var issued = issue("gql.inv.active@mail.com", memberRole);
		addMember(tenant, invitee, null);

		var response = graphqlAs(ACCEPT, codeInput(issued.code()), invitee);
		assertGraphQlError(response, "BAD_REQUEST");
		response.expectBody().jsonPath("$.errors[0].message").isEqualTo("You are already a member of this tenant");
	}

	@Test
	void accepting_withAnInactiveMembership_pointsToReactivation() {
		var invitee = createUser("Inactive Member", "gql.inv.inactive@mail.com");
		var issued = issue("gql.inv.inactive@mail.com", memberRole);
		addMember(tenant, invitee, null);
		graphqlIn(tenant, admin, """
				mutation($input: TenantUserIdInput!) { inactivateTenantUser(input: $input) { tenantUser { active } } }""",
				variables("input", variables("userId", invitee.user().getId())))
				.expectBody().jsonPath("$.errors").doesNotExist();

		graphqlAs(ACCEPT, codeInput(issued.code()), invitee)
				.expectBody()
				.jsonPath("$.errors[0].extensions.classification").isEqualTo("BAD_REQUEST")
				.jsonPath("$.errors[0].message").value(m -> assertThat(m.toString()).contains("inactive"));
	}

	@Test
	void aRemovedMember_whoIsInvitedAgain_getsTheMembershipRestored() {
		var returning = createUser("Returning Member", "gql.inv.returning@mail.com");
		var firstRole = newRole(tenant, admin, "First Stint", List.of("tenant_roles:read"));
		var secondRole = newRole(tenant, admin, "Second Stint", List.of());
		var first = issue("gql.inv.returning@mail.com", firstRole);
		graphqlAs(ACCEPT, codeInput(first.code()), returning).expectBody().jsonPath("$.errors").doesNotExist();

		// Removed by an administrator; they lose access straight away.
		graphqlIn(tenant, admin, """
				mutation($input: TenantUserIdInput!) { removeTenantUser(input: $input) { removedUserId } }""",
				variables("input", variables("userId", returning.user().getId())))
				.expectBody().jsonPath("$.errors").doesNotExist();
		assertGraphQlError(graphqlIn(tenant, returning, "{ tenantRoles(first: 1) { edges { node { id } } } }",
				variables()), "FORBIDDEN");
		assertThat(memberEmails(tenant, admin)).doesNotContain("gql.inv.returning@mail.com");

		var second = issue("gql.inv.returning@mail.com", secondRole);
		graphqlAs(ACCEPT, codeInput(second.code()), returning).expectBody().jsonPath("$.errors").doesNotExist();

		graphqlIn(tenant, admin, MEMBERS, variables())
				.expectBody()
				.jsonPath("$.data.tenantUsers.edges[?(@.node.user.email == 'gql.inv.returning@mail.com')].node.roles[0].name")
				.isEqualTo("Second Stint")
				.jsonPath("$.data.tenantUsers.edges[?(@.node.user.email == 'gql.inv.returning@mail.com')].node.active")
				.isEqualTo(true);
	}

	// ---- revoking and expiring

	@Test
	void revoke_cancelsThePendingInvitation() {
		var issued = issue("gql.inv.revoked@mail.com", memberRole);

		graphqlIn(tenant, admin, REVOKE, variables("input", variables("invitationId", issued.id())))
				.expectBody()
				.jsonPath("$.errors").doesNotExist()
				.jsonPath("$.data.revokeInvitation.invitation.status").isEqualTo("REVOKED");
		graphqlIn(tenant, admin, REVOKE, variables("input", variables("invitationId", issued.id())))
				.expectBody().jsonPath("$.errors").doesNotExist();

		assertGraphQlError(graphql(CHECK, codeVars(issued.code())), "NOT_FOUND");
		var invitee = createUser("Was Revoked", "gql.inv.revoked@mail.com");
		assertGraphQlError(graphqlAs(ACCEPT, codeInput(issued.code()), invitee), "NOT_FOUND");
	}

	@Test
	void expiredInvitation_behavesLikeAnUnknownOne() {
		var issued = issue("gql.inv.expired@mail.com", memberRole);
		expireNow(issued.id());

		assertGraphQlError(graphql(CHECK, codeVars(issued.code())), "NOT_FOUND");
		graphqlIn(tenant, admin, INVITATIONS, variables("status", "EXPIRED"))
				.expectBody()
				.jsonPath("$.data.tenantInvitations.edges[?(@.node.id == '%s')]".formatted(issued.id())).isNotEmpty();
		graphqlIn(tenant, admin, INVITATIONS, variables("status", "PENDING"))
				.expectBody()
				.jsonPath("$.data.tenantInvitations.edges[?(@.node.id == '%s')]".formatted(issued.id())).isEmpty();
	}

	// ---- access control

	// ---- listing

	@Test
	void listing_pagesForwardAndRolesAreBatched() {
		var listTenant = newTenant(admin, "Listing Tenant");
		var role = newRole(listTenant, admin, "Listed", List.of());
		for (int i = 1; i <= 5; i++) {
			graphqlIn(listTenant, admin, INVITE, inviteVars("gql.inv.list" + i + "@mail.com", role))
					.expectBody().jsonPath("$.errors").doesNotExist();
		}

		var pages = collectPages("tenantInvitations", "email",
				after -> graphqlIn(listTenant, admin, INVITATIONS, variables("first", 2, "after", after)));
		assertThat(pages.values()).containsExactly("gql.inv.list1@mail.com", "gql.inv.list2@mail.com",
				"gql.inv.list3@mail.com", "gql.inv.list4@mail.com", "gql.inv.list5@mail.com");
		assertThat(pages.count()).isEqualTo(3);

		assertStatementCountIndependentOfPageSize(
				() -> graphqlIn(listTenant, admin, INVITATIONS, variables("first", 1))
						.expectBody().jsonPath("$.errors").doesNotExist(),
				() -> graphqlIn(listTenant, admin, INVITATIONS, variables("first", 5))
						.expectBody().jsonPath("$.errors").doesNotExist());
	}

	// ---- search and invite by roles

	private static final String SEARCH = """
			query($query: String) {
				tenantInvitations(first: 100, query: $query) { edges { node { email } } }
			}""";
	private static final String DEACTIVATE_ROLE = """
			mutation($input: TenantRoleIdInput!) { inactivateTenantRole(input: $input) { tenantRole { id active } } }""";

	private List<String> searchEmails(String query) {
		var emails = new ArrayList<String>();
		graphqlIn(tenant, admin, SEARCH, variables("query", query))
				.expectBody()
				.jsonPath("$.errors").doesNotExist()
				.jsonPath("$.data.tenantInvitations.edges[*].node.email")
				.value(v -> ((List<?>) v).forEach(e -> emails.add(e.toString())));
		return emails;
	}

	@Test
	void invitations_canBeSearchedByEmail() {
		issue("gql.inv.search.alice@mail.com", memberRole);
		issue("gql.inv.search.bob@mail.com", memberRole);

		assertThat(searchEmails("ALICE")).containsExactly("gql.inv.search.alice@mail.com");
		assertThat(searchEmails("gql.inv.search")).containsExactlyInAnyOrder("gql.inv.search.alice@mail.com",
				"gql.inv.search.bob@mail.com");
		assertThat(searchEmails("%")).isEmpty();
		assertThat(searchEmails("nobody-matches")).isEmpty();
	}

	@Test
	void invite_withAnInactiveRole_isRejected() {
		var retired = newRole(tenant, admin, "Retired", List.of("tenant_roles:read"));
		graphqlIn(tenant, admin, DEACTIVATE_ROLE, variables("input", variables("id", retired)))
				.expectBody().jsonPath("$.errors").doesNotExist();

		assertGraphQlError(graphqlIn(tenant, admin, INVITE, inviteVars("gql.inv.retired@mail.com", retired)),
				"BAD_REQUEST");
		assertGraphQlError(graphqlIn(tenant, admin, INVITE, inviteVars("gql.inv.retired@mail.com", memberRole, retired)),
				"BAD_REQUEST");
	}

	@Test
	void invite_cannotGrantARoleWithPermissionsTheCallerLacks() {
		var inviter = createUser("Limited Inviter", "gql.inv.limitedinviter@mail.com");
		var inviterRole = newRole(tenant, admin, "Inviter only", List.of("tenants:invite"));
		addMember(tenant, inviter, inviterRole);
		var powerful = newRole(tenant, admin, "Powerful", List.of("tenants:invite", "tenants:expel"));

		graphqlIn(tenant, inviter, INVITE, inviteVars("gql.inv.grant.ok@mail.com", inviterRole))
				.expectBody().jsonPath("$.errors").doesNotExist();
		assertGraphQlError(graphqlIn(tenant, inviter, INVITE, inviteVars("gql.inv.grant.no@mail.com", powerful)),
				"FORBIDDEN");
	}

	// ---- resending

	private static final String RESEND = """
			mutation($input: ResendInvitationInput!) {
				resendInvitation(input: $input) { invitation { id status expiresAt } }
			}""";

	private java.util.Map<String, Object> idInput(UUID id) {
		return variables("input", variables("invitationId", id));
	}

	@Test
	void resend_emailsAFreshLinkForTheSameInvitation() {
		var issued = issue("gql.inv.resend@mail.com", memberRole);

		graphqlIn(tenant, admin, RESEND, idInput(issued.id()))
				.expectBody()
				.jsonPath("$.errors").doesNotExist()
				.jsonPath("$.data.resendInvitation.invitation.id").isEqualTo(issued.id().toString())
				.jsonPath("$.data.resendInvitation.invitation.status").isEqualTo("PENDING");

		var newCode = codeSentTo("gql.inv.resend@mail.com");
		assertThat(newCode).isNotEqualTo(issued.code());
		assertGraphQlError(graphql(CHECK, codeVars(issued.code())), "NOT_FOUND");
		graphql(CHECK, codeVars(newCode)).expectBody().jsonPath("$.errors").doesNotExist();
	}

	@Test
	void resend_revivesAnExpiredInvitation() {
		var issued = issue("gql.inv.resendexp@mail.com", memberRole);
		expireNow(issued.id());

		graphqlIn(tenant, admin, RESEND, idInput(issued.id()))
				.expectBody()
				.jsonPath("$.errors").doesNotExist()
				.jsonPath("$.data.resendInvitation.invitation.status").isEqualTo("PENDING");

		graphql(CHECK, codeVars(codeSentTo("gql.inv.resendexp@mail.com")))
				.expectBody().jsonPath("$.errors").doesNotExist();
	}

	@Test
	void resend_isRejectedForARevokedInvitation() {
		var revoked = issue("gql.inv.resendrev@mail.com", memberRole);
		graphqlIn(tenant, admin, REVOKE, idInput(revoked.id())).expectBody().jsonPath("$.errors").doesNotExist();
		assertGraphQlError(graphqlIn(tenant, admin, RESEND, idInput(revoked.id())), "BAD_REQUEST");
	}

	/** Revoking and resending share the rules for which invitations they can reach. */
	@ParameterizedTest
	@ValueSource(strings = { "revoke", "resend" })
	void changingAnInvitation_isRejectedWhenAcceptedUnknownOrForeign(String operation) {
		var document = operation.equals("revoke") ? REVOKE : RESEND;
		var email = "gql.inv.%s.accepted@mail.com".formatted(operation);

		var invitee = createUser("Accepted " + operation, email);
		var accepted = issue(email, memberRole);
		graphqlAs(ACCEPT, codeInput(accepted.code()), invitee).expectBody().jsonPath("$.errors").doesNotExist();
		assertGraphQlError(graphqlIn(tenant, admin, document, idInput(accepted.id())), "BAD_REQUEST");

		assertGraphQlError(graphqlIn(tenant, admin, document, idInput(UUID.randomUUID())), "NOT_FOUND");

		var pending = issue("gql.inv.%s.foreign@mail.com".formatted(operation), memberRole);
		var outsiderTenant = newTenant(outsider, "Outsider Tenant for " + operation);
		assertGraphQlError(graphqlIn(outsiderTenant, outsider, document, idInput(pending.id())), "NOT_FOUND");
	}
}
