package com.example.demo.tenant;

import org.junit.jupiter.api.BeforeAll;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.client.RestTestClient;

import com.example.demo.TestcontainersConfiguration;
import com.example.demo.tenant.dto.CreateTenantRequest;
import com.example.demo.user.User;
import com.example.demo.user.UserRepository;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@AutoConfigureRestTestClient
@Import(TestcontainersConfiguration.class)
class TenantUserControllerTest {
    User mainUser;
    String mainUserEmail = "mainuser@example.com";
    User auxUser;
    String auxUserEmail = "auxuser@example.com";

    Tenant tenant;

    private final UserRepository userRepository;
    private final TenantUserService tenantUserService;
    private final TenantService tenantService;

    private final RestTestClient restClient;

    public TenantUserControllerTest(
            UserRepository userRepository,
            TenantUserService tenantUserService,
            TenantService tenantService,
            RestTestClient restClient) {
        this.userRepository = userRepository;
        this.restClient = restClient;
        this.tenantUserService = tenantUserService;
        this.tenantService = tenantService;
    }

    @BeforeAll
    void populateUsers() {
        mainUser = userRepository.save(new User("mainUser", mainUserEmail, "Password@123"));
        auxUser = userRepository.save(new User("auxUser", mainUserEmail, "Password@123"));
        var tenantId = tenantService.createTenant(new CreateTenantRequest("Test Tenant"), mainUser.getId()).tenantId();
        tenant = tenantService.findTenantById(tenantId).orElseThrow();
    }
}
