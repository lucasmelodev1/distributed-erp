package com.example.demo.user;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.springframework.web.client.RestClient;

import com.example.demo.BaseControllerTest;
import com.example.demo.TestcontainersConfiguration;
import com.example.demo.user.dto.CreateUserByAuthTokenRequest;
import com.example.demo.user.dto.FindMeResponse;
import com.example.demo.user.dto.LoginRequest;
import com.example.demo.user.dto.LoginResponse;
import com.example.demo.user.dto.RequestEmailVerificationRequest;
import com.example.demo.user.dto.RequestEmailVerificationResponse;
import com.example.demo.user.dto.ResetPasswordRequest;
import com.example.demo.user.dto.VerifyEmailRequest;
import com.example.demo.user.dto.VerifyEmailResponse;
import com.example.demo.user.dto.VerifyResetPasswordResponse;
import com.example.demo.utils.TokenUtils;

class AuthControllerTest extends BaseControllerTest {
	private String exampleName = "Example User";
	private String exampleEmail = "demo@example.com";
	private String examplePassword = "Password@123";

	@Test
	void fullFlow_validData_returnsOkForAll() {
		/**
		 * Testing the user registration flow
		 */
		post("/api/v1/auth/register-email-verification")
				.body(new RequestEmailVerificationRequest(exampleEmail))
				.exchange()
				.expectStatus().isOk()
				.expectBody(RequestEmailVerificationResponse.class);
		post("/api/v1/auth/register-verify-email")
				.body(new VerifyEmailRequest(TokenUtils.getAuthTestToken()))
				.exchange()
				.expectStatus().isOk()
				.expectBody(VerifyEmailResponse.class);
		post("/api/v1/auth/register-create-user")
				.body(new CreateUserByAuthTokenRequest(exampleName, TokenUtils.getAuthTestToken(),
						examplePassword))
				.exchange()
				.expectStatus().isOk()
				.expectCookie().exists(SESSION_NAME)
				.expectBody(LoginResponse.class);

		/**
		 * Testing the user login flow and data validity
		 */
		var cookies = post("/api/v1/auth/login")
				.body(new LoginRequest(exampleEmail, examplePassword))
				.exchange()
				.expectStatus().isOk()
				.expectCookie().exists(SESSION_NAME)
				.expectBody(LoginResponse.class)
				.returnResult().getResponseCookies();
		var sessionCookie = cookies.getFirst(SESSION_NAME);
		if (sessionCookie == null) {
			throw new RuntimeException("SESSION cookie not found after login");
		}
		var findMeResponse = get("/api/v1/user/me")
				.cookie(SESSION_NAME, sessionCookie.getValue())
				.exchange()
				.expectStatus().isOk()
				.expectBody(FindMeResponse.class)
				.returnResult().getResponseBody();

		assert findMeResponse != null;
		assert findMeResponse.email().equals(exampleEmail);
		assert findMeResponse.name().equals(exampleName);

		post("/api/v1/auth/logout")
				.cookie(SESSION_NAME, sessionCookie.getValue())
				.exchange()
				.expectStatus().isOk();
		get("/api/v1/user/me")
				.cookie(SESSION_NAME, sessionCookie.getValue())
				.exchange()
				.expectStatus().isUnauthorized();

		/**
		 * Tests the reset password feature
		 */
		post("/api/v1/auth/request-reset-password")
				.body(new RequestEmailVerificationRequest(exampleEmail))
				.exchange()
				.expectStatus().isOk();
		post("/api/v1/auth/verify-reset-password")
				.body(new VerifyEmailRequest(TokenUtils.getAuthTestToken()))
				.exchange()
				.expectStatus().isOk()
				.expectBody(VerifyResetPasswordResponse.class);
		var newPassword = "NewPassword@123";
		post("/api/v1/auth/reset-password")
				.body(new ResetPasswordRequest(exampleEmail, TokenUtils.getAuthTestToken(), newPassword))
				.exchange()
				.expectStatus().isOk();

		post("/api/v1/auth/login")
				.body(new LoginRequest(exampleEmail, newPassword))
				.exchange()
				.expectStatus().isOk()
				.expectCookie().exists(SESSION_NAME)
				.expectBody(LoginResponse.class);
	}

	static Stream<RequestEmailVerificationRequest> requestEmailVerification_invalidEmails() {
		return Stream.of(
				new RequestEmailVerificationRequest("not-an-email"),
				new RequestEmailVerificationRequest(""),
				new RequestEmailVerificationRequest("invalid-email@"),
				new RequestEmailVerificationRequest("invalid-email.com"),
				new RequestEmailVerificationRequest("invalid-email@com"),
				new RequestEmailVerificationRequest(null));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("requestEmailVerification_invalidEmails")
	void requestEmailVerification_invalidEmails_returns400(RequestEmailVerificationRequest dto) {
		post("/api/v1/auth/register-email-verification")
				.body(dto)
				.exchange()
				.expectStatus().isBadRequest();
	}

	static Stream<VerifyEmailRequest> verifyEmail_invalidTokens() {
		return Stream.of(
				new VerifyEmailRequest(""),
				new VerifyEmailRequest("123"),
				new VerifyEmailRequest("invalid-token"),
				new VerifyEmailRequest(null));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("verifyEmail_invalidTokens")
	void verifyEmail_invalidTokens_returns400(VerifyEmailRequest dto) {
		post("/api/v1/auth/register-verify-email")
				.body(dto)
				.exchange()
				.expectStatus().isBadRequest();
	}

	static Stream<CreateUserByAuthTokenRequest> createUser_invalidData() {
		var testToken = TokenUtils.getAuthTestToken();
		return Stream.of(
				new CreateUserByAuthTokenRequest("", testToken, "Password@123"),
				new CreateUserByAuthTokenRequest("Name", "", "Password@123"),
				new CreateUserByAuthTokenRequest("Name", testToken, ""),
				new CreateUserByAuthTokenRequest("Name", testToken, "a".repeat(User.PASSWORD_MIN_SIZE - 1)),
				new CreateUserByAuthTokenRequest("Name", testToken, "a".repeat(User.PASSWORD_MAX_SIZE + 1)),
				new CreateUserByAuthTokenRequest("a".repeat(User.NAME_MAX_SIZE + 1), testToken, "Password@123"));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("createUser_invalidData")
	void createUser_invalidData_returns400(CreateUserByAuthTokenRequest dto) {
		post("/api/v1/auth/register-create-user")
				.body(dto)
				.exchange()
				.expectStatus().isBadRequest();
	}
}
