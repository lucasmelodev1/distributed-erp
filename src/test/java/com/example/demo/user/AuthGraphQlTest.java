package com.example.demo.user;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import com.example.demo.BaseGraphQlTest;
import com.example.demo.user.domain.AuthToken;
import com.example.demo.user.domain.User;
import com.example.demo.utils.TokenService;

class AuthGraphQlTest extends BaseGraphQlTest {
	@Autowired
	private TokenService tokenService;

	private static final String NAME = "Graph User";
	private static final String EMAIL = "graphql.auth@example.com";
	private static final String PASSWORD = "Password@123";

	private static final String LOGIN = """
			mutation($input: LoginInput!) { login(input: $input) { id email role } }""";
	private static final String ME = "{ me { id name email profileIconUrl } }";

	private static Map<String, Object> input(Object... keyValues) {
		var input = new java.util.HashMap<String, Object>();
		for (int i = 0; i < keyValues.length; i += 2) {
			input.put((String) keyValues[i], keyValues[i + 1]);
		}
		return Map.of("input", input);
	}

	@Test
	void fullFlow_validData_succeedsForAll() {
		/* Registration: every token is the one the user received, never a hash */
		graphql("""
				mutation($input: RequestEmailVerificationInput!) {
					requestEmailVerification(input: $input) { authTokenId }
				}""", input("email", EMAIL))
				.expectStatus().isOk()
				.expectBody()
				.jsonPath("$.errors").doesNotExist()
				.jsonPath("$.data.requestEmailVerification.authTokenId").exists();
		var emailedToken = sentEmails.lastTextTo(EMAIL);
		assertThat(emailedToken).isEqualTo(tokenService.generate()).hasSize(AuthToken.TOKEN_SIZE);

		var verifiedToken = new String[1];
		graphql("""
				mutation($input: VerifyEmailInput!) { verifyEmail(input: $input) { token } }""",
				input("token", emailedToken))
				.expectBody()
				.jsonPath("$.errors").doesNotExist()
				.jsonPath("$.data.verifyEmail.token").value(t -> verifiedToken[0] = t.toString());
		assertThat(verifiedToken[0]).isEqualTo(tokenService.generate()).hasSize(AuthToken.TOKEN_SIZE);

		graphql("""
				mutation($input: RegisterUserInput!) { registerUser(input: $input) { id email role } }""",
				input("name", NAME, "token", verifiedToken[0], "password", PASSWORD))
				.expectStatus().isOk()
				.expectCookie().exists(SESSION_NAME)
				.expectBody()
				.jsonPath("$.errors").doesNotExist()
				.jsonPath("$.data.registerUser.email").isEqualTo(EMAIL);

		/* Login, me, logout */
		var sessionId = graphql(LOGIN, input("email", EMAIL, "password", PASSWORD))
				.expectStatus().isOk()
				.expectCookie().exists(SESSION_NAME)
				.expectBody()
				.jsonPath("$.errors").doesNotExist()
				.jsonPath("$.data.login.email").isEqualTo(EMAIL)
				.returnResult()
				.getResponseCookies().getFirst(SESSION_NAME).getValue();

		graphql(ME, Map.of(), sessionId, null)
				.expectBody()
				.jsonPath("$.errors").doesNotExist()
				.jsonPath("$.data.me.email").isEqualTo(EMAIL)
				.jsonPath("$.data.me.name").isEqualTo(NAME)
				.jsonPath("$.data.me.profileIconUrl").isEmpty();

		graphql("mutation { logout { success } }", Map.of(), sessionId, null)
				.expectBody()
				.jsonPath("$.data.logout.success").isEqualTo(true);
		assertGraphQlError(graphql(ME, Map.of(), sessionId, null), "UNAUTHORIZED");

		/* Password reset */
		graphql("""
				mutation($input: RequestPasswordResetInput!) {
					requestPasswordReset(input: $input) { authTokenId }
				}""", input("email", EMAIL))
				.expectBody()
				.jsonPath("$.errors").doesNotExist();
		var resetRequestToken = sentEmails.lastTextTo(EMAIL);
		assertThat(resetRequestToken).hasSize(AuthToken.TOKEN_SIZE);

		var resetToken = new String[1];
		graphql("""
				mutation($input: VerifyPasswordResetInput!) { verifyPasswordReset(input: $input) { token } }""",
				input("token", resetRequestToken))
				.expectBody()
				.jsonPath("$.errors").doesNotExist()
				.jsonPath("$.data.verifyPasswordReset.token").value(t -> resetToken[0] = t.toString());
		assertThat(resetToken[0]).hasSize(AuthToken.TOKEN_SIZE);
		var newPassword = "NewPassword@123";
		graphql("""
				mutation($input: ResetPasswordInput!) { resetPassword(input: $input) { success } }""",
				input("email", EMAIL, "token", resetToken[0], "password", newPassword))
				.expectBody()
				.jsonPath("$.errors").doesNotExist()
				.jsonPath("$.data.resetPassword.success").isEqualTo(true);

		graphql(LOGIN, input("email", EMAIL, "password", newPassword))
				.expectCookie().exists(SESSION_NAME)
				.expectBody()
				.jsonPath("$.errors").doesNotExist();
		assertGraphQlError(graphql(LOGIN, input("email", EMAIL, "password", PASSWORD)), "UNAUTHORIZED");
	}

	@Test
	void requestEmailVerification_emailsTheTokenNotItsHash() {
		graphql("""
				mutation($input: RequestEmailVerificationInput!) {
					requestEmailVerification(input: $input) { authTokenId }
				}""", input("email", "gql.hash.check@example.com"))
				.expectBody()
				.jsonPath("$.errors").doesNotExist();

		var emailed = sentEmails.lastTextTo("gql.hash.check@example.com");
		assertThat(emailed).hasSize(AuthToken.TOKEN_SIZE);
		assertThat(emailed).isNotEqualTo(com.example.demo.utils.HashUtils.hash(tokenService.generate()));
	}

	@Test
	void me_withoutSession_isUnauthorized() {
		assertGraphQlError(graphql(ME), "UNAUTHORIZED");
	}

	@Test
	void logout_withoutSession_isUnauthorized() {
		assertGraphQlError(graphql("mutation { logout { success } }"), "UNAUTHORIZED");
	}

	@Test
	void login_withUnknownUser_isUnauthorized() {
		assertGraphQlError(
				graphql(LOGIN, input("email", "nobody@example.com", "password", PASSWORD)), "UNAUTHORIZED");
	}

	@Test
	void requestPasswordReset_unknownEmail_doesNotRevealWhetherItExists() {
		graphql("""
				mutation($input: RequestPasswordResetInput!) {
					requestPasswordReset(input: $input) { authTokenId }
				}""", input("email", "unknown.person@example.com"))
				.expectBody()
				.jsonPath("$.errors").doesNotExist()
				.jsonPath("$.data.requestPasswordReset.authTokenId").exists();
	}

	static Stream<String> invalidEmails() {
		return Stream.of("not-an-email", "", "invalid-email@", "invalid-email.com", "invalid-email@com");
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("invalidEmails")
	void requestEmailVerification_invalidEmail_isBadRequest(String email) {
		assertGraphQlError(graphql("""
				mutation($input: RequestEmailVerificationInput!) {
					requestEmailVerification(input: $input) { authTokenId }
				}""", input("email", email)), "BAD_REQUEST");
	}

	@Test
	void requestEmailVerification_missingEmail_isRejectedBySchema() {
		graphql("""
				mutation($input: RequestEmailVerificationInput!) {
					requestEmailVerification(input: $input) { authTokenId }
				}""", input())
				.expectBody()
				.jsonPath("$.data").doesNotExist()
				.jsonPath("$.errors[0]").exists();
	}

	static Stream<String> invalidTokens() {
		return Stream.of("", "123", "invalid-token");
	}

	@ParameterizedTest(name = "[{0}]")
	@MethodSource("invalidTokens")
	void verifyEmail_invalidTokenSize_isBadRequest(String token) {
		assertGraphQlError(graphql("""
				mutation($input: VerifyEmailInput!) { verifyEmail(input: $input) { token } }""",
				input("token", token)), "BAD_REQUEST");
	}

	@Test
	void verifyEmail_unknownToken_isNotFound() {
		assertGraphQlError(graphql("""
				mutation($input: VerifyEmailInput!) { verifyEmail(input: $input) { token } }""",
				input("token", "zzzzzz")), "NOT_FOUND");
	}

	static Stream<Map<String, Object>> invalidRegistrations() {
		return Stream.of(
				Map.of("name", "", "token", "000000", "password", "Password@123"),
				Map.of("name", "Name", "token", "", "password", "Password@123"),
				Map.of("name", "Name", "token", "000000", "password", ""),
				Map.of("name", "Name", "token", "000000", "password", "a".repeat(User.PASSWORD_MIN_SIZE - 1)),
				Map.of("name", "Name", "token", "000000", "password", "a".repeat(User.PASSWORD_MAX_SIZE + 1)),
				Map.of("name", "a".repeat(User.NAME_MAX_SIZE + 1), "token", "000000", "password", "Password@123"));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("invalidRegistrations")
	void registerUser_invalidData_isBadRequest(Map<String, Object> registration) {
		assertGraphQlError(graphql("""
				mutation($input: RegisterUserInput!) { registerUser(input: $input) { id } }""",
				Map.of("input", registration)), "BAD_REQUEST");
	}
}
