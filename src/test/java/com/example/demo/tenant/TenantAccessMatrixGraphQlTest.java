package com.example.demo.tenant;

import static org.junit.jupiter.api.Assertions.fail;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.util.ClassUtils;

import com.example.demo.BaseGraphQlTest;
import com.example.demo.TestUser;
import com.example.demo.tenant.config.TenantAware;
import com.netflix.graphql.dgs.DgsComponent;
import com.netflix.graphql.dgs.DgsMutation;
import com.netflix.graphql.dgs.DgsQuery;

/**
 * Every {@code @TenantAware} operation must refuse callers who are not entitled to it. The operations and the
 * permissions they need are read from the data fetchers, so adding an operation without a row in
 * {@link #DOCUMENTS} fails the build instead of silently skipping the check.
 */
class TenantAccessMatrixGraphQlTest extends BaseGraphQlTest {
	private static final String ANONYMOUS = "anonymous";
	private static final String NO_TENANT_HEADER = "no tenant header";
	private static final String OUTSIDER = "not a member";
	private static final String INACTIVE_MEMBER = "inactive member";
	private static final String MISSING = "missing ";

	/** Placeholder arguments: access is decided before an operation looks at them. */
	private static final UUID SOME_ID = UUID.randomUUID();
	private static final Map<String, Object> ID_INPUT = variables("input", variables("id", SOME_ID));
	private static final Map<String, Object> USER_INPUT = variables("input", variables("userId", SOME_ID));
	private static final Map<String, Object> INVITATION_INPUT = variables("input",
			variables("invitationId", SOME_ID));
	private static final Map<String, Object> NONE = variables();

	private record Operation(String document, Map<String, Object> variables) {
	}

	private static final Map<String, Operation> DOCUMENTS = new LinkedHashMap<>();

	private static void query(String name, String selection, Map<String, Object> variables, String... declared) {
		var declaration = declared.length == 0 ? "" : "(" + String.join(", ", declared) + ")";
		DOCUMENTS.put(name, new Operation("query%s { %s }".formatted(declaration, selection), variables));
	}

	private static void mutation(String name, String inputType, String selection, Map<String, Object> variables) {
		var declaration = inputType == null ? "" : "($input: %s!)".formatted(inputType);
		var argument = inputType == null ? "" : "(input: $input)";
		DOCUMENTS.put(name, new Operation("mutation%s { %s%s { %s } }".formatted(declaration, name, argument,
				selection), variables));
	}

	static {
		query("currentTenant", "currentTenant { id }", NONE);
		mutation("updateTenant", "UpdateTenantInput", "tenant { id }", variables("input", variables("name", "x")));
		mutation("inactivateTenant", null, "tenant { id }", NONE);
		mutation("reactivateTenant", null, "tenant { id }", NONE);

		query("tenantRoles", "tenantRoles { pageInfo { hasNextPage } }", NONE);
		query("permissions", "permissions { code }", NONE);
		query("tenantRole", "tenantRole(id: $id) { id }", variables("id", SOME_ID), "$id: UUID!");
		mutation("createTenantRole", "CreateTenantRoleInput", "tenantRole { id }",
				variables("input", variables("name", "x", "permissionCodes", List.of())));
		mutation("updateTenantRole", "UpdateTenantRoleInput", "tenantRole { id }",
				variables("input", variables("id", SOME_ID, "name", "x", "permissionCodes", List.of())));
		mutation("inactivateTenantRole", "TenantRoleIdInput", "tenantRole { id }", ID_INPUT);
		mutation("reactivateTenantRole", "TenantRoleIdInput", "tenantRole { id }", ID_INPUT);

		query("tenantUsers", "tenantUsers { pageInfo { hasNextPage } }", NONE);
		query("tenantUser", "tenantUser(userId: $userId) { active }", variables("userId", SOME_ID),
				"$userId: UUID!");
		mutation("updateTenantUser", "UpdateTenantUserInput", "tenantUser { active }",
				variables("input", variables("userId", SOME_ID, "tenantRoleIds", List.of())));
		mutation("inactivateTenantUser", "TenantUserIdInput", "tenantUser { active }", USER_INPUT);
		mutation("reactivateTenantUser", "TenantUserIdInput", "tenantUser { active }", USER_INPUT);
		mutation("removeTenantUser", "TenantUserIdInput", "removedUserId", USER_INPUT);

		query("tenantInvitations", "tenantInvitations { pageInfo { hasNextPage } }", NONE);
		mutation("inviteUserToTenant", "InviteUserToTenantInput", "invitation { id }",
				variables("input", variables("email", "matrix@mail.com", "tenantRoleIds", List.of(SOME_ID))));
		mutation("revokeInvitation", "RevokeInvitationInput", "invitation { id }", INVITATION_INPUT);
		mutation("resendInvitation", "ResendInvitationInput", "invitation { id }", INVITATION_INPUT);
	}

	/** Operation name to the permissions it requires, read from the {@code @TenantAware} data fetchers. */
	private static Map<String, List<String>> tenantAwareOperations() {
		var scanner = new ClassPathScanningCandidateComponentProvider(false);
		scanner.addIncludeFilter(new AnnotationTypeFilter(DgsComponent.class));
		var operations = new LinkedHashMap<String, List<String>>();
		for (BeanDefinition definition : scanner.findCandidateComponents("com.example.demo")) {
			if (definition.getBeanClassName().endsWith("TestOperations")) {
				continue; // test-only operations of GraphQlInfrastructureTest
			}
			Class<?> type = ClassUtils.resolveClassName(definition.getBeanClassName(), null);
			for (Method method : type.getDeclaredMethods()) {
				var tenantAware = AnnotatedElementUtils.findMergedAnnotation(method, TenantAware.class);
				boolean operation = method.isAnnotationPresent(DgsQuery.class)
						|| method.isAnnotationPresent(DgsMutation.class);
				if (tenantAware != null && operation) {
					operations.put(method.getName(), List.of(tenantAware.withPermissions()));
				}
			}
		}
		return operations;
	}

	private static final Map<String, List<String>> OPERATIONS = tenantAwareOperations();

	static Stream<Arguments> denials() {
		Set<String> undocumented = new LinkedHashSet<>(OPERATIONS.keySet());
		undocumented.removeAll(DOCUMENTS.keySet());
		Set<String> stale = new LinkedHashSet<>(DOCUMENTS.keySet());
		stale.removeAll(OPERATIONS.keySet());
		if (!undocumented.isEmpty() || !stale.isEmpty()) {
			fail("DOCUMENTS is out of sync with the @TenantAware operations; add %s, remove %s"
					.formatted(undocumented, stale));
		}

		var cases = new ArrayList<Arguments>();
		OPERATIONS.forEach((operation, permissions) -> {
			cases.add(Arguments.of(operation, ANONYMOUS, "UNAUTHORIZED"));
			cases.add(Arguments.of(operation, NO_TENANT_HEADER, "UNAUTHORIZED"));
			cases.add(Arguments.of(operation, OUTSIDER, "FORBIDDEN"));
			cases.add(Arguments.of(operation, INACTIVE_MEMBER, "FORBIDDEN"));
			for (var permission : permissions) {
				cases.add(Arguments.of(operation, MISSING + permission, "FORBIDDEN"));
			}
		});
		return cases.stream();
	}

	private static Set<String> everyRequiredPermission() {
		var all = new LinkedHashSet<String>();
		OPERATIONS.values().forEach(all::addAll);
		return all;
	}

	private TestUser owner;
	private TestUser outsider;
	private TestUser inactiveMember;
	private UUID tenant;
	private final Map<String, TestUser> missingPermission = new HashMap<>();

	@BeforeAll
	void setup() {
		owner = createUser("Matrix Owner", "gql.matrix.owner@mail.com");
		outsider = createUser("Matrix Outsider", "gql.matrix.outsider@mail.com");
		inactiveMember = createUser("Matrix Inactive", "gql.matrix.inactive@mail.com");
		tenant = newTenant(owner, "Matrix Tenant");

		// Holds everything, but the membership is switched off.
		var everything = newRole(tenant, owner, "Everything", List.copyOf(everyRequiredPermission()));
		addMember(tenant, inactiveMember, everything);
		graphqlIn(tenant, owner, """
				mutation($input: TenantUserIdInput!) { inactivateTenantUser(input: $input) { tenantUser { active } } }""",
				variables("input", variables("userId", inactiveMember.user().getId())))
				.expectBody().jsonPath("$.errors").doesNotExist();

		// One member per permission, holding every other permission: proves each listed permission is needed.
		for (var permission : everyRequiredPermission()) {
			var others = everyRequiredPermission().stream().filter(p -> !p.equals(permission)).toList();
			var role = newRole(tenant, owner, "All but " + permission, others);
			var member = createUser("Missing " + permission, "gql.matrix.missing." + permission.replaceAll("\\W", ".") + "@mail.com");
			addMember(tenant, member, role);
			missingPermission.put(permission, member);
		}
	}

	@ParameterizedTest(name = "{0}: {1} gets {2}")
	@MethodSource("denials")
	void operation_isDeniedToCallersWhoAreNotEntitled(String operation, String caller, String code) {
		var request = DOCUMENTS.get(operation);
		var response = switch (caller) {
			case ANONYMOUS -> graphql(request.document(), request.variables(), null, tenant.toString());
			case NO_TENANT_HEADER -> graphql(request.document(), request.variables(), owner.cookie().getValue(),
					null);
			case OUTSIDER -> graphqlIn(tenant, outsider, request.document(), request.variables());
			case INACTIVE_MEMBER -> graphqlIn(tenant, inactiveMember, request.document(), request.variables());
			default -> graphqlIn(tenant, missingPermission.get(caller.substring(MISSING.length())),
					request.document(), request.variables());
		};
		assertGraphQlError(response, code);
	}
}
