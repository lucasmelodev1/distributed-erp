package com.example.demo.tenant;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.BaseIntegrationTest;
import com.example.demo.tenant.dto.CreateTenantRequest;
import com.example.demo.tenant.dto.UpdateTenantRequest;
import com.example.demo.user.User;
import com.example.demo.user.UserRepository;

@Transactional
class TenantControllerTest extends BaseIntegrationTest {

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private TenantRepository tenantRepository;

	private static final String TEST_EMAIL = "test@example.com";
	private static final String TEST_PASSWORD = "Password@123";

	private User getOrCreateUser(String email) {
		return userRepository.findByEmail(email).orElseGet(() -> {
			User user = new User("Test User", email, passwordEncoder.encode(TEST_PASSWORD));
			return userRepository.save(user);
		});
	}

	private UUID createTenantDirectly(String name, User user) throws Exception {
		var result = mockMvc.perform(post("/api/v1/tenant")
				.with(SecurityMockMvcRequestPostProcessors.user(user))
				.contentType(MediaType.APPLICATION_JSON)
				.content(toJson(new CreateTenantRequest(name))))
				.andReturn();

		String responseBody = result.getResponse().getContentAsString();
		String tenantId = responseBody.replaceAll(".*\"tenantId\":\"([^\"]+)\".*", "$1");
		return UUID.fromString(tenantId);
	}

	@Nested
	class FindAllUserTenants {

		@Test
		void findAllUserTenants_withAuth_returnsOk() throws Exception {
			User user = getOrCreateUser(TEST_EMAIL);
			UUID tenantId = createTenantDirectly("Find All Tenant", user);

			mockMvc.perform(get("/api/v1/tenant")
					.with(SecurityMockMvcRequestPostProcessors.user(user)))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$[0].id").value(tenantId.toString()));
		}

		@Test
		void findAllUserTenants_noAuth_returns401() throws Exception {
			mockMvc.perform(get("/api/v1/tenant"))
					.andExpect(status().isUnauthorized());
		}
	}

	@Nested
	class CreateTenant {

		@Test
		void createTenant_validRequest_returnsOk() throws Exception {
			User user = getOrCreateUser(TEST_EMAIL);

			mockMvc.perform(post("/api/v1/tenant")
					.with(SecurityMockMvcRequestPostProcessors.user(user))
					.contentType(MediaType.APPLICATION_JSON)
					.content(toJson(new CreateTenantRequest("New Tenant"))))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.tenantId").isNotEmpty());
		}

		@Test
		void createTenant_noAuth_returns401() throws Exception {
			mockMvc.perform(post("/api/v1/tenant")
					.contentType(MediaType.APPLICATION_JSON)
					.content(toJson(new CreateTenantRequest("New Tenant"))))
					.andExpect(status().isUnauthorized());
		}

		@Test
		void createTenant_blankName_returns400() throws Exception {
			User user = getOrCreateUser(TEST_EMAIL);

			mockMvc.perform(post("/api/v1/tenant")
					.with(SecurityMockMvcRequestPostProcessors.user(user))
					.contentType(MediaType.APPLICATION_JSON)
					.content(toJson(new CreateTenantRequest(""))))
					.andExpect(status().isBadRequest());
		}
	}

	@Nested
	class UpdateTenant {

		@Test
		void updateTenant_validRequest_returnsOk() throws Exception {
			User user = getOrCreateUser(TEST_EMAIL);
			UUID tenantId = createTenantDirectly("Update Tenant", user);

			mockMvc.perform(put("/api/v1/tenant")
					.with(SecurityMockMvcRequestPostProcessors.user(user))
					.contentType(MediaType.APPLICATION_JSON)
					.content(toJson(new UpdateTenantRequest("Updated Tenant")))
					.header("X-Tenant-Id", tenantId.toString()))
					.andExpect(status().isOk());
		}

		@Test
		void updateTenant_missingTenantHeader_returns401() throws Exception {
			User user = getOrCreateUser(TEST_EMAIL);

			mockMvc.perform(put("/api/v1/tenant")
					.with(SecurityMockMvcRequestPostProcessors.user(user))
					.contentType(MediaType.APPLICATION_JSON)
					.content(toJson(new UpdateTenantRequest("Updated"))))
					.andExpect(status().isUnauthorized())
					.andExpect(jsonPath("$.error").value("Missing required header: X-Tenant-Id"));
		}

		@Test
		void updateTenant_notMember_returns401() throws Exception {
			User otherUser = getOrCreateUser("other@example.com");
			UUID tenantId = createTenantDirectly("Other Tenant", otherUser);

			User nonMemberUser = getOrCreateUser("nonmember@example.com");

			mockMvc.perform(put("/api/v1/tenant")
					.with(SecurityMockMvcRequestPostProcessors.user(nonMemberUser))
					.contentType(MediaType.APPLICATION_JSON)
					.content(toJson(new UpdateTenantRequest("Updated")))
					.header("X-Tenant-Id", tenantId.toString()))
					.andExpect(status().isUnauthorized())
					.andExpect(jsonPath("$.error").value("You are not a member of this tenant"));
		}

		@Test
		void updateTenant_invalidUuid_returns400() throws Exception {
			User user = getOrCreateUser(TEST_EMAIL);

			mockMvc.perform(put("/api/v1/tenant")
					.with(SecurityMockMvcRequestPostProcessors.user(user))
					.contentType(MediaType.APPLICATION_JSON)
					.content(toJson(new UpdateTenantRequest("Updated")))
					.header("X-Tenant-Id", "not-a-uuid"))
					.andExpect(status().isBadRequest())
					.andExpect(jsonPath("$.error").value("Invalid UUID format for header: X-Tenant-Id"));
		}
	}

	@Nested
	class DeleteTenant {

		@Test
		void deleteTenant_validRequest_returnsOk() throws Exception {
			User user = getOrCreateUser(TEST_EMAIL);
			UUID tenantId = createTenantDirectly("Delete Tenant", user);

			mockMvc.perform(delete("/api/v1/tenant")
					.with(SecurityMockMvcRequestPostProcessors.user(user))
					.header("X-Tenant-Id", tenantId.toString()))
					.andExpect(status().isOk());

			var deletedTenant = tenantRepository.findById(tenantId);
			assert deletedTenant.isEmpty();
		}

		@Test
		void deleteTenant_missingTenantHeader_returns401() throws Exception {
			User user = getOrCreateUser(TEST_EMAIL);

			mockMvc.perform(delete("/api/v1/tenant")
					.with(SecurityMockMvcRequestPostProcessors.user(user)))
					.andExpect(status().isUnauthorized())
					.andExpect(jsonPath("$.error").value("Missing required header: X-Tenant-Id"));
		}

		@Test
		void deleteTenant_notMember_returns401() throws Exception {
			User otherUser = getOrCreateUser("other-delete@example.com");
			UUID tenantId = createTenantDirectly("Other Delete Tenant", otherUser);

			User nonMemberUser = getOrCreateUser("nonmember-delete@example.com");

			mockMvc.perform(delete("/api/v1/tenant")
					.with(SecurityMockMvcRequestPostProcessors.user(nonMemberUser))
					.header("X-Tenant-Id", tenantId.toString()))
					.andExpect(status().isUnauthorized())
					.andExpect(jsonPath("$.error").value("You are not a member of this tenant"));
		}

	}
}
