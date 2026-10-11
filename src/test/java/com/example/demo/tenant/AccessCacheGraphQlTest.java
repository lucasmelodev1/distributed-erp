package com.example.demo.tenant;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;

import org.hibernate.SessionFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;

import com.example.demo.BaseGraphQlTest;
import com.example.demo.TestUser;
import com.example.demo.tenant.adapter.outbound.persistence.TenantUserRoleRepository;
import com.example.demo.tenant.domain.authorization.TenantUserRoleId;

import jakarta.persistence.EntityManagerFactory;

/** Access data is cached in the session and only reloaded after a change to tenants, roles or permissions. */
class AccessCacheGraphQlTest extends BaseGraphQlTest {
	private static final String PERMISSIONS = "query { permissions { code } }";
	private static final String INACTIVATE_ROLE = """
			mutation($input: TenantRoleIdInput!) { inactivateTenantRole(input: $input) { tenantRole { id } } }""";
	private static final String REACTIVATE_ROLE = """
			mutation($input: TenantRoleIdInput!) { reactivateTenantRole(input: $input) { tenantRole { id } } }""";
	private static final String REACTIVATE_USER = """
			mutation($input: TenantUserIdInput!) { reactivateTenantUser(input: $input) { tenantUser { user { id } } } }""";
	private static final String UPDATE_ROLE = """
			mutation($input: UpdateTenantRoleInput!) { updateTenantRole(input: $input) { tenantRole { id } } }""";
	private static final String INACTIVATE_USER = """
			mutation($input: TenantUserIdInput!) { inactivateTenantUser(input: $input) { tenantUser { user { id } } } }""";
	private static final String REMOVE_USER = """
			mutation($input: TenantUserIdInput!) { removeTenantUser(input: $input) { removedUserId } }""";
	private static final String INACTIVATE_TENANT = "mutation { inactivateTenant { tenant { id } } }";

	@Autowired
	private TenantUserRoleRepository tenantUserRoleRepository;
	@Autowired
	private EntityManagerFactory entityManagerFactory;
	@Autowired
	private StringRedisTemplate redis;

	private record Holder(UUID tenantId, UUID roleId, TestUser owner, TestUser member) {
	}

	/** A member whose only role grants tenant_roles:read. */
	private Holder holder(String suffix) {
		var owner = createUser("Cache Owner " + suffix, "cache.owner." + suffix + "@mail.com");
		var member = createUser("Cache Member " + suffix, "cache.member." + suffix + "@mail.com");
		var tenant = newTenant(owner, "Cache Tenant " + suffix);
		var role = newRole(tenant, owner, "Reader " + suffix, List.of("tenant_roles:read"));
		addMember(tenant, member, role);
		return new Holder(tenant, role, owner, member);
	}

	private void assertAllowed(Holder h, TestUser user) {
		graphqlIn(h.tenantId(), user, PERMISSIONS, variables()).expectBody().jsonPath("$.errors").doesNotExist();
	}

	@Test
	void repeatedRequests_doNotQueryAccessDataAgain() {
		var h = holder("queries");
		var statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
		statistics.setStatisticsEnabled(true);
		try {
			statistics.clear();
			assertAllowed(h, h.member());
			long first = statistics.getPrepareStatementCount();

			statistics.clear();
			assertAllowed(h, h.member());
			long second = statistics.getPrepareStatementCount();

			assertThat(second).isLessThan(first);
		} finally {
			statistics.setStatisticsEnabled(false);
		}
	}

	@Test
	void inactivatingRole_takesEffectOnNextRequest() {
		var h = holder("role");
		assertAllowed(h, h.member());

		graphqlIn(h.tenantId(), h.owner(), INACTIVATE_ROLE, variables("input", variables("id", h.roleId())))
				.expectBody().jsonPath("$.errors").doesNotExist();

		assertGraphQlError(graphqlIn(h.tenantId(), h.member(), PERMISSIONS, variables()), "FORBIDDEN");

		graphqlIn(h.tenantId(), h.owner(), REACTIVATE_ROLE, variables("input", variables("id", h.roleId())))
				.expectBody().jsonPath("$.errors").doesNotExist();
		assertAllowed(h, h.member());
	}

	@Test
	void changingRolePermissions_takesEffectOnNextRequest() {
		var h = holder("perms");
		assertAllowed(h, h.member());

		graphqlIn(h.tenantId(), h.owner(), UPDATE_ROLE, variables("input",
				variables("id", h.roleId(), "name", "Reader perms", "permissionCodes", List.of("tenants:update"))))
				.expectBody().jsonPath("$.errors").doesNotExist();

		assertGraphQlError(graphqlIn(h.tenantId(), h.member(), PERMISSIONS, variables()), "FORBIDDEN");
	}

	@Test
	void inactivatingMember_takesEffectOnNextRequest() {
		var h = holder("member");
		assertAllowed(h, h.member());

		graphqlIn(h.tenantId(), h.owner(), INACTIVATE_USER,
				variables("input", variables("userId", h.member().user().getId())))
				.expectBody().jsonPath("$.errors").doesNotExist();

		var denied = graphqlIn(h.tenantId(), h.member(), PERMISSIONS, variables());
		assertGraphQlError(denied, "FORBIDDEN");
		denied.expectBody().jsonPath("$.errors[0].message").isEqualTo("Your membership in this tenant is inactive");

		graphqlIn(h.tenantId(), h.owner(), REACTIVATE_USER,
				variables("input", variables("userId", h.member().user().getId())))
				.expectBody().jsonPath("$.errors").doesNotExist();
		assertAllowed(h, h.member());
	}

	@Test
	void removingMember_takesEffectOnNextRequest() {
		var h = holder("removed");
		assertAllowed(h, h.member());

		graphqlIn(h.tenantId(), h.owner(), REMOVE_USER,
				variables("input", variables("userId", h.member().user().getId())))
				.expectBody().jsonPath("$.errors").doesNotExist();

		assertGraphQlError(graphqlIn(h.tenantId(), h.member(), PERMISSIONS, variables()), "FORBIDDEN");
	}

	@Test
	void inactivatingTenant_affectsEveryMemberOnNextRequest() {
		var h = holder("tenant");
		assertAllowed(h, h.member());

		graphqlIn(h.tenantId(), h.owner(), INACTIVATE_TENANT, variables())
				.expectBody().jsonPath("$.errors").doesNotExist();

		assertGraphQlError(graphqlIn(h.tenantId(), h.member(), PERMISSIONS, variables()), "FORBIDDEN");
	}

	@Test
	void newTenant_isVisibleToAnAlreadyCachedSession() {
		var user = createUser("Cache New", "cache.new@mail.com");
		var first = newTenant(user, "First Cached");
		assertAllowed(new Holder(first, null, user, user), user);

		var second = newTenant(user, "Second Cached");

		assertAllowed(new Holder(second, null, user, user), user);
	}

	@Test
	void lostEpochKeys_neverRevalidateAStaleSnapshot() {
		var h = holder("epochs");
		assertAllowed(h, h.member());

		// Revoke behind the services' back: the cached snapshot still allows it.
		tenantUserRoleRepository.deleteById(new TenantUserRoleId(h.member().user().getId(), h.tenantId(), h.roleId()));
		assertAllowed(h, h.member());

		// Redis loses its epoch keys; the snapshot must not be trusted any more.
		redis.delete(redis.keys("access:epoch:*"));
		assertGraphQlError(graphqlIn(h.tenantId(), h.member(), PERMISSIONS, variables()), "FORBIDDEN");
	}
}
