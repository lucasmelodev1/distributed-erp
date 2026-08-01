package com.example.demo.tenant;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.example.demo.BaseControllerTest;
import com.example.demo.TestUser;
import com.example.demo.tenant.dto.CreateTenantRequest;
import com.example.demo.tenant.dto.CreateTenantResponse;

public class TenantControllerTest extends BaseControllerTest {

    public TenantControllerTest() {
        super("/api/v1/tenant");
    }

    private TestUser alphaUser;
    private TestUser betaUser;

    @Autowired
    private TenantRepository tenantRepository;

    @BeforeAll
    void setup() {
        alphaUser = createUser("Alpha User", "alphauser@mail.com");
        betaUser = createUser("Beta User", "betauser@mail.com");
    }

    @Test
    void fullFlow_validData_returnsOkForAll() {
        getAs("", alphaUser)
                .exchange()
                .expectStatus().isOk()
                .expectBody(List.class)
                .value(tenants -> {
                    assertEquals(tenants.size(), 0);
                });
        var alphaTenantName = "Alpha Tenant";

        var alphaCreateTenantResponse = postAs("", alphaUser)
                .body(new CreateTenantRequest(alphaTenantName))
                .exchange()
                .expectStatus().isCreated()
                .expectBody(CreateTenantResponse.class)
                .value(response -> {
                    assertNotNull(response.id());
                    assertEquals(response.name(), alphaTenantName);
                    assertEquals(response.users().size(), 1);
                    assertEquals(response.roles().size(), 1);
                    assertEquals(response.users().get(0), alphaUser.user().getId());
                })
                .returnResult().getResponseBody();
        var alphaTenant = tenantRepository.findById(alphaCreateTenantResponse.id()).orElseThrow();

        var newAlphaTenantName = "New Alpha Tenant";
        putWithTenant("", alphaUser, alphaTenant)
                .body(new CreateTenantRequest(newAlphaTenantName))
                .exchange()
                .expectStatus().isOk();
        getWithTenant("/current", alphaUser, alphaTenant)
                .exchange()
                .expectStatus().isOk()
                .expectBody(Tenant.class)
                .value(response -> {
                    assertEquals(response.getName(), newAlphaTenantName);
                });
    }
}
