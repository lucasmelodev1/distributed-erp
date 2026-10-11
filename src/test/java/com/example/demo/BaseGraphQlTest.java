package com.example.demo;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

import org.hibernate.SessionFactory;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.client.RestTestClient;

import com.example.demo.email.RecordingEmailSender;
import com.example.demo.email.SentEmails;
import com.example.demo.tenant.adapter.outbound.persistence.TenantUserRepository;
import com.example.demo.tenant.adapter.outbound.persistence.TenantUserRoleRepository;
import com.example.demo.tenant.config.AccessCache;
import com.example.demo.tenant.config.TenantContext;
import com.example.demo.tenant.domain.TenantUser;
import com.example.demo.tenant.domain.TenantUserId;
import com.example.demo.tenant.domain.authorization.TenantUserRole;
import com.example.demo.tenant.domain.authorization.TenantUserRoleId;
import com.example.demo.user.adapter.outbound.persistence.AuthTokenRepository;
import com.example.demo.user.adapter.outbound.persistence.UserRepository;
import com.example.demo.user.adapter.web.dto.user.CreateUserInput;
import com.example.demo.user.port.inbound.UserProfilePort;

import jakarta.persistence.EntityManagerFactory;

/**
 * Base for tests that call the GraphQL endpoint over HTTP on a random port with
 * the test profile and the Testcontainers services.
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Import(TestcontainersConfiguration.class)
public abstract class BaseGraphQlTest {
	protected static final String SESSION_NAME = "SESSION";
	protected static final String TENANT_HEADER = TenantContext.TENANT_ID_HEADER;
	protected static final String TEST_PASSWORD = "Password@123";

	private static final String LOGIN = """
			mutation($input: LoginInput!) { login(input: $input) { id } }""";

	@Autowired
	protected RestTestClient restClient;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private UserProfilePort userService;

	@Autowired
	private AuthTokenRepository authTokenRepository;

	@Autowired
	private TenantUserRepository tenantUserRepository;

	@Autowired
	private TenantUserRoleRepository tenantUserRoleRepository;

	@Autowired
	private AccessCache accessCache;

	@Autowired
	private EntityManagerFactory entityManagerFactory;

	@Autowired
	protected SentEmails sentEmails;

	@Autowired
	protected RecordingEmailSender emailSender;

	@Autowired
	protected KafkaAdmin kafkaAdmin;

	/**
	 * In the test profile every generated token is the same value, so tokens left
	 * over by other test classes sharing the database would make lookups ambiguous.
	 */
	@BeforeEach
	void clearAuthTokens() {
		// Emails travel through Kafka: let the previous test's messages (and retries) land before clearing,
		// or a late one would show up in this test.
		awaitEmailsProcessed();
		emailSender.reset();
		authTokenRepository.deleteAll();
		sentEmails.clear();
	}

	/** Waits until the email consumer has processed everything published so far, including retries. */
	protected void awaitEmailsProcessed() {
		KafkaTestSupport.awaitEmailsProcessed(kafkaAdmin);
	}

	/** Creates the user through the service and logs in through GraphQL to obtain a session. */
	protected TestUser createUser(String name, String email) {
		var created = userService.createUser(new CreateUserInput(name, email, TEST_PASSWORD));
		var cookie = graphql(LOGIN, variables("input", variables("email", email, "password", TEST_PASSWORD)))
				.expectStatus().isOk()
				.expectBody()
				.jsonPath("$.errors").doesNotExist()
				.returnResult()
				.getResponseCookies()
				.getFirst(SESSION_NAME);
		return new TestUser(userRepository.findById(created.id()).orElseThrow(), cookie);
	}

	/** Anonymous request. */
	protected RestTestClient.ResponseSpec graphql(String document) {
		return graphql(document, Map.of(), null, null);
	}

	protected RestTestClient.ResponseSpec graphql(String document, Map<String, Object> variables) {
		return graphql(document, variables, null, null);
	}

	protected RestTestClient.ResponseSpec graphqlAs(String document, Map<String, Object> variables, TestUser user) {
		return graphql(document, variables, user.cookie().getValue(), null);
	}

	protected RestTestClient.ResponseSpec graphql(String document, Map<String, Object> variables,
			@Nullable String sessionId, @Nullable String tenantHeader) {
		var request = restClient.post()
				.uri("/graphql")
				.contentType(MediaType.APPLICATION_JSON)
				.accept(MediaType.APPLICATION_JSON);
		if (sessionId != null) {
			request = request.cookie(SESSION_NAME, sessionId);
		}
		if (tenantHeader != null) {
			request = request.header(TENANT_HEADER, tenantHeader);
		}
		return request.body(Map.of("query", document, "variables", variables)).exchange();
	}

	/** Builds a variables map; null values are kept (unlike Map.of). */
	protected static Map<String, Object> variables(Object... keyValues) {
		var map = new java.util.HashMap<String, Object>();
		for (int i = 0; i < keyValues.length; i += 2) {
			map.put((String) keyValues[i], keyValues[i + 1]);
		}
		return map;
	}

	/** Request as a member of a tenant: sends the session cookie and the X-Tenant-Id header. */
	protected RestTestClient.ResponseSpec graphqlIn(UUID tenantId, TestUser user, String document,
			Map<String, Object> variables) {
		return graphql(document, variables, user.cookie().getValue(), tenantId.toString());
	}

	protected UUID newTenant(TestUser user, String name) {
		var id = new UUID[1];
		graphqlAs("mutation($input: CreateTenantInput!) { createTenant(input: $input) { tenant { id } } }",
				variables("input", variables("name", name)), user)
				.expectBody()
				.jsonPath("$.errors").doesNotExist()
				.jsonPath("$.data.createTenant.tenant.id").value(v -> id[0] = UUID.fromString(v.toString()));
		return id[0];
	}

	protected UUID newRole(UUID tenantId, TestUser user, String name,
			java.util.List<String> permissionCodes) {
		var id = new UUID[1];
		graphqlIn(tenantId, user, """
				mutation($input: CreateTenantRoleInput!) { createTenantRole(input: $input) { tenantRole { id } } }""",
				variables("input", variables("name", name, "permissionCodes", permissionCodes)))
				.expectBody()
				.jsonPath("$.errors").doesNotExist()
				.jsonPath("$.data.createTenantRole.tenantRole.id")
				.value(v -> id[0] = UUID.fromString(v.toString()));
		return id[0];
	}

	/** Makes the user a member of the tenant, holding the role if one is given. */
	protected void addMember(UUID tenantId, TestUser user, UUID roleId) {
		addMemberWithRoles(tenantId, user, roleId == null ? List.<UUID>of() : List.of(roleId));
	}

	/**
	 * Makes the user a member of the tenant holding the roles. The rows are written behind the services' back, so
	 * the user's cached access is invalidated here.
	 */
	protected void addMemberWithRoles(UUID tenantId, TestUser user, List<UUID> roleIds) {
		tenantUserRepository.save(new TenantUser(new TenantUserId(tenantId, user.user().getId())));
		for (var roleId : roleIds) {
			tenantUserRoleRepository
					.save(new TenantUserRole(new TenantUserRoleId(user.user().getId(), tenantId, roleId)));
		}
		accessCache.invalidateUser(user.user().getId());
	}

	/** The values of every page of a connection, and how many pages it took. */
	protected record Pages(List<String> values, int count) {
	}

	/**
	 * Follows a forward connection to its end. {@code request} runs the query for the given cursor (null for the
	 * first page); the result holds {@code node.nodeField} of every edge, in order.
	 */
	protected Pages collectPages(String connection, String nodeField,
			Function<String, RestTestClient.ResponseSpec> request) {
		var values = new ArrayList<String>();
		var path = "$.data." + connection;
		String after = null;
		boolean hasNext = true;
		int count = 0;
		while (hasNext) {
			var next = new Object[2];
			request.apply(after).expectBody()
					.jsonPath("$.errors").doesNotExist()
					.jsonPath(path + ".edges[*].node." + nodeField)
					.value(v -> ((List<?>) v).forEach(e -> values.add(e.toString())))
					.jsonPath(path + ".pageInfo.hasNextPage").value(v -> next[0] = v)
					.jsonPath(path + ".pageInfo.endCursor").value(v -> next[1] = v);
			hasNext = (Boolean) next[0];
			after = (String) next[1];
			assertThat(++count).as("number of pages").isLessThan(20);
		}
		return new Pages(values, count);
	}

	/** Proves nested fields are batched: a larger page must not run more SQL statements than a page of one. */
	protected void assertStatementCountIndependentOfPageSize(Runnable smallPage, Runnable largePage) {
		var statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
		statistics.setStatisticsEnabled(true);
		try {
			smallPage.run(); // warm up caches and lazily built state
			statistics.clear();
			smallPage.run();
			long small = statistics.getPrepareStatementCount();
			statistics.clear();
			largePage.run();
			long large = statistics.getPrepareStatementCount();
			assertThat(small).isGreaterThan(0);
			assertThat(large).isEqualTo(small);
		} finally {
			statistics.setStatisticsEnabled(false);
		}
	}

	/** GraphQL reports failures in the body with HTTP 200. */
	protected void assertGraphQlError(RestTestClient.ResponseSpec response, String classification) {
		response.expectStatus().isOk()
				.expectBody()
				.jsonPath("$.errors[0].extensions.classification").isEqualTo(classification)
				.jsonPath("$.errors[0].extensions.code").isEqualTo(classification);
	}
}
