package com.example.demo;

import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.client.RestTestClient;
import com.example.demo.tenant.domain.Tenant;
import com.example.demo.tenant.config.TenantValidationInterceptor;
import com.example.demo.user.adapter.outbound.persistence.UserRepository;
import com.example.demo.user.adapter.inbound.UserService;
import com.example.demo.user.adapter.web.dto.user.CreateUserRequest;
import com.example.demo.user.adapter.web.dto.auth.LoginRequest;
import com.example.demo.user.adapter.web.dto.auth.LoginResponse;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@AutoConfigureRestTestClient
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Import(TestcontainersConfiguration.class)
public abstract class BaseControllerTest {
    @Autowired
    protected RestTestClient restClient;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

    protected final String baseUrl;

    protected static final String SESSION_NAME = "SESSION";
    protected static final String TENANT_HEADER = TenantValidationInterceptor.TENANT_ID_HEADER;
    protected static final String TEST_PASSWORD = "Password@123";

    private String getFormattedUri(String uri) {
        return "%s%s".formatted(baseUrl, uri);
    }

    public BaseControllerTest(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    protected TestUser createUser(String name, String email) {
        var response = userService.createUser(new CreateUserRequest(name, email, TEST_PASSWORD));
        var cookie = postRaw("/api/v1/auth/login")
                .body(new LoginRequest(email, TEST_PASSWORD))
                .exchange()
                .expectStatus().isOk()
                .returnResult(LoginResponse.class)
                .getResponseCookies()
                .getFirst(SESSION_NAME);
        return new TestUser(userRepository.findById(response.userId()).orElseThrow(), cookie);
    }

    protected RestTestClient.RequestBodySpec postRaw(String uri) {
        return restClient.post()
                .uri(uri)
                .accept(MediaType.APPLICATION_JSON);
    }

    protected RestTestClient.RequestBodySpec post(String uri) {
        return restClient.post()
                .uri(getFormattedUri(uri))
                .accept(MediaType.APPLICATION_JSON);
    }

    protected RestTestClient.RequestBodySpec postAs(String uri, TestUser user) {
        return restClient.post()
                .uri(getFormattedUri(uri))
                .cookie(SESSION_NAME, user.cookie().getValue())
                .accept(MediaType.APPLICATION_JSON);
    }

    protected RestTestClient.RequestBodySpec postWithTenant(String uri, TestUser user, Tenant tenant) {
        return restClient.post()
                .uri(getFormattedUri(uri))
                .cookie(SESSION_NAME, user.cookie().getValue())
                .header(TENANT_HEADER, tenant.getId().toString())
                .accept(MediaType.APPLICATION_JSON);
    }

    protected RestTestClient.RequestBodySpec put(String uri) {
        return restClient.put()
                .uri(getFormattedUri(uri))
                .accept(MediaType.APPLICATION_JSON);
    }

    protected RestTestClient.RequestBodySpec putAs(String uri, TestUser user) {
        return restClient.put()
                .uri(getFormattedUri(uri))
                .cookie(SESSION_NAME, user.cookie().getValue())
                .accept(MediaType.APPLICATION_JSON);
    }

    protected RestTestClient.RequestBodySpec putWithTenant(String uri, TestUser user, Tenant tenant) {
        return restClient.put()
                .uri(getFormattedUri(uri))
                .cookie(SESSION_NAME, user.cookie().getValue())
                .header(TENANT_HEADER, tenant.getId().toString())
                .accept(MediaType.APPLICATION_JSON);
    }

    protected RestTestClient.RequestHeadersSpec<?> delete(String uri) {
        return restClient.delete()
                .uri(getFormattedUri(uri))
                .accept(MediaType.APPLICATION_JSON);
    }

    protected RestTestClient.RequestHeadersSpec<?> deleteAs(String uri, TestUser user) {
        return restClient.delete()
                .uri(getFormattedUri(uri))
                .cookie(SESSION_NAME, user.cookie().getValue())
                .accept(MediaType.APPLICATION_JSON);
    }

    protected RestTestClient.RequestHeadersSpec<?> deleteWithTenant(String uri, TestUser user, Tenant tenant) {
        return restClient.delete()
                .uri(getFormattedUri(uri))
                .cookie(SESSION_NAME, user.cookie().getValue())
                .header(TENANT_HEADER, tenant.getId().toString())
                .accept(MediaType.APPLICATION_JSON);
    }

    protected RestTestClient.RequestHeadersSpec<?> get(String uri) {
        return restClient.get()
                .uri(getFormattedUri(uri))
                .accept(MediaType.APPLICATION_JSON);
    }

    protected RestTestClient.RequestHeadersSpec<?> getAs(String uri, TestUser user) {
        return restClient.get()
                .uri(getFormattedUri(uri))
                .cookie(SESSION_NAME, user.cookie().getValue())
                .accept(MediaType.APPLICATION_JSON);
    }

    protected RestTestClient.RequestHeadersSpec<?> getWithTenant(String uri, TestUser user, Tenant tenant) {
        return restClient.get()
                .uri(getFormattedUri(uri))
                .cookie(SESSION_NAME, user.cookie().getValue())
                .header(TENANT_HEADER, tenant.getId().toString())
                .accept(MediaType.APPLICATION_JSON);
    }
}
