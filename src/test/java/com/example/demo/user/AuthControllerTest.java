package com.example.demo.user;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import com.example.demo.BaseControllerTest;
import com.example.demo.user.domain.User;
import com.example.demo.user.adapter.web.dto.auth.CreateUserByAuthTokenRequest;
import com.example.demo.user.adapter.web.dto.user.FindMeResponse;
import com.example.demo.user.adapter.web.dto.auth.LoginRequest;
import com.example.demo.user.adapter.web.dto.auth.LoginResponse;
import com.example.demo.user.adapter.web.dto.auth.RequestEmailVerificationRequest;
import com.example.demo.user.adapter.web.dto.auth.RequestEmailVerificationResponse;
import com.example.demo.user.adapter.web.dto.auth.ResetPasswordRequest;
import com.example.demo.user.adapter.web.dto.auth.VerifyEmailRequest;
import com.example.demo.user.adapter.web.dto.auth.VerifyEmailResponse;
import com.example.demo.user.adapter.web.dto.auth.VerifyResetPasswordResponse;
import com.example.demo.utils.TokenService;

class AuthControllerTest extends BaseControllerTest {
	@Autowired
	private TokenService tokenService;

	public AuthControllerTest() {
		super("/api/v1/auth");
	}

	private String exampleName = "Example User";
	private String exampleEmail = "demo@example.com";
	private String examplePassword = "Password@123";

	@Test
	void fullFlow_validData_returnsOkForAll() {
		/**
		 * Testing the user registration flow
		 */
		post("/register-request-email-verification")
				.body(new RequestEmailVerificationRequest(exampleEmail))
				.exchange()
				.expectStatus().isOk()
				.expectBody(RequestEmailVerificationResponse.class);
		post("/register-verify-email")
				.body(new VerifyEmailRequest(tokenService.generate()))
				.exchange()
				.expectStatus().isOk()
				.expectBody(VerifyEmailResponse.class);
		post("/register-create-user")
				.body(new CreateUserByAuthTokenRequest(exampleName, tokenService.generate(),
						examplePassword))
				.exchange()
				.expectStatus().isOk()
				.expectCookie().exists(SESSION_NAME)
				.expectBody(LoginResponse.class);

		/**
		 * Testing the user login flow and data validity
		 */
		var cookies = post("/login")
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
		var findMeResponse = get("/me")
				.cookie(SESSION_NAME, sessionCookie.getValue())
				.exchange()
				.expectStatus().isOk()
				.expectBody(FindMeResponse.class)
				.returnResult().getResponseBody();

		assert findMeResponse != null;
		assert findMeResponse.email().equals(exampleEmail);
		assert findMeResponse.name().equals(exampleName);

		post("/logout")
				.cookie(SESSION_NAME, sessionCookie.getValue())
				.exchange()
				.expectStatus().isOk();
		get("/me")
				.cookie(SESSION_NAME, sessionCookie.getValue())
				.exchange()
				.expectStatus().isUnauthorized();

		/**
		 * Tests the reset password feature
		 */
		post("/request-reset-password")
				.body(new RequestEmailVerificationRequest(exampleEmail))
				.exchange()
				.expectStatus().isOk();
		post("/verify-reset-password")
				.body(new VerifyEmailRequest(tokenService.generate()))
				.exchange()
				.expectStatus().isOk()
				.expectBody(VerifyResetPasswordResponse.class);
		var newPassword = "NewPassword@123";
		post("/reset-password")
				.body(new ResetPasswordRequest(exampleEmail, tokenService.generate(), newPassword))
				.exchange()
				.expectStatus().isOk();

		post("/login")
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
		post("/register-request-email-verification")
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
		post("/register-verify-email")
				.body(dto)
				.exchange()
				.expectStatus().isBadRequest();
	}

	Stream<CreateUserByAuthTokenRequest> createUser_invalidData() {
		var testToken = tokenService.generate();
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
		post("/register-create-user")
				.body(dto)
				.exchange()
				.expectStatus().isBadRequest();
	}
}
