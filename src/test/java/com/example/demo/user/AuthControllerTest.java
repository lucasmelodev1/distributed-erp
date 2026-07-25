package com.example.demo.user;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.example.demo.BaseIntegrationTest;
import com.example.demo.user.dto.CreateUserRequest;
import com.example.demo.user.dto.LoginRequest;
import com.example.demo.user.dto.RequestEmailVerificationRequest;
import com.example.demo.user.dto.RequestResetPasswordRequest;
import com.example.demo.user.dto.ResetPasswordRequest;
import com.example.demo.user.dto.VerifyEmailRequest;
import com.example.demo.user.dto.VerifyPasswordResetRequestRequest;
import com.example.demo.utils.TokenUtils;

import jakarta.persistence.EntityManager;

class AuthControllerTest extends BaseIntegrationTest {

	@Autowired
	private AuthTokenRepository authTokenRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private EntityManager entityManager;

	private String createEmailVerificationToken(String email) {
		String token = TokenUtils.generateNumeric(AuthToken.TOKEN_SIZE);
		authTokenRepository.save(new AuthToken(email, TokenUtils.hash(token), AuthTokenType.EMAIL_VERIFICATION));
		entityManager.flush();
		return token;
	}

	private String createUserCreationToken(String email) {
		String token = TokenUtils.generateNumeric(AuthToken.TOKEN_SIZE);
		authTokenRepository.save(new AuthToken(email, TokenUtils.hash(token), AuthTokenType.USER_CREATION));
		entityManager.flush();
		return token;
	}

	private String createPasswordResetRequestToken(String email) {
		String token = TokenUtils.generateNumeric(AuthToken.TOKEN_SIZE);
		authTokenRepository.save(new AuthToken(email, TokenUtils.hash(token), AuthTokenType.PASSWORD_RESET_REQUEST));
		entityManager.flush();
		return token;
	}

	private String createPasswordResetToken(String email) {
		String token = TokenUtils.generateNumeric(AuthToken.TOKEN_SIZE);
		authTokenRepository.save(new AuthToken(email, TokenUtils.hash(token), AuthTokenType.PASSWORD_RESET));
		entityManager.flush();
		return token;
	}

	private User createUserInDb(String email, String password, String name) {
		var user = new User(name, email, passwordEncoder.encode(password));
		userRepository.save(user);
		entityManager.flush();
		return user;
	}

	@Nested
	class RegisterEmailVerification {

		@Test
		void requestEmailVerification_validEmail_returnsOk() throws Exception {
			performPost("/api/v1/auth/register-email-verification",
					new RequestEmailVerificationRequest("test@example.com"))
					.andExpect(status().isOk());
		}

		@Test
		void requestEmailVerification_invalidEmail_returns400() throws Exception {
			performPost("/api/v1/auth/register-email-verification",
					new RequestEmailVerificationRequest("not-an-email"))
					.andExpect(status().isBadRequest());
		}

		@Test
		void requestEmailVerification_blankEmail_returns400() throws Exception {
			performPost("/api/v1/auth/register-email-verification",
					new RequestEmailVerificationRequest(""))
					.andExpect(status().isBadRequest());
		}

		@Test
		void requestEmailVerification_createsTokenInDb() throws Exception {
			String email = "tokencheck@example.com";

			performPost("/api/v1/auth/register-email-verification",
					new RequestEmailVerificationRequest(email))
					.andExpect(status().isOk());

			var tokens = authTokenRepository.findActiveVerifications(
					email, AuthTokenType.EMAIL_VERIFICATION, Instant.now());
			assert tokens.size() == 1;
			assert tokens.getFirst().getEmail().equals(email);
		}

		@Test
		void requestEmailVerification_calledTwice_revokesOldToken() throws Exception {
			String email = "revoke@example.com";

			performPost("/api/v1/auth/register-email-verification",
					new RequestEmailVerificationRequest(email))
					.andExpect(status().isOk());

			performPost("/api/v1/auth/register-email-verification",
					new RequestEmailVerificationRequest(email))
					.andExpect(status().isOk());

			var activeTokens = authTokenRepository.findActiveVerifications(
					email, AuthTokenType.EMAIL_VERIFICATION, Instant.now());
			assert activeTokens.size() == 1 : "Only the newest token should be active";
		}
	}

	@Nested
	class VerifyEmail {

		@Test
		void verifyEmail_validToken_returnsOkAndNewToken() throws Exception {
			String email = "verify@example.com";
			String token = createEmailVerificationToken(email);

			performPost("/api/v1/auth/register-verify-email",
					new VerifyEmailRequest(token))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.token").isNotEmpty());
		}

		@Test
		void verifyEmail_invalidToken_returns404() throws Exception {
			performPost("/api/v1/auth/register-verify-email",
					new VerifyEmailRequest("999999"))
					.andExpect(status().isNotFound());
		}

		@Test
		void verifyEmail_blankToken_returns400() throws Exception {
			performPost("/api/v1/auth/register-verify-email",
					new VerifyEmailRequest(""))
					.andExpect(status().isBadRequest());
		}

		@Test
		void verifyEmail_wrongSizeToken_returns400() throws Exception {
			performPost("/api/v1/auth/register-verify-email",
					new VerifyEmailRequest("123"))
					.andExpect(status().isBadRequest());
		}

		@Test
		void verifyEmail_consumedToken_returns404() throws Exception {
			String email = "consumed@example.com";
			String token = createEmailVerificationToken(email);

			// First verification succeeds
			performPost("/api/v1/auth/register-verify-email",
					new VerifyEmailRequest(token))
					.andExpect(status().isOk());

			// Second verification with same token fails
			performPost("/api/v1/auth/register-verify-email",
					new VerifyEmailRequest(token))
					.andExpect(status().isNotFound());
		}
	}

	@Nested
	class CreateUser {

		@Test
		void createUser_validToken_returnsOk() throws Exception {
			String email = "newuser@example.com";
			String token = createUserCreationToken(email);

			performPost("/api/v1/auth/register-create-user",
					new CreateUserRequest("New User", token, "Password@123"))
					.andExpect(status().isOk());

			assert userRepository.findByEmail(email).isPresent();
		}

		@Test
		void createUser_validToken_savesCorrectData() throws Exception {
			String email = "savedata@example.com";
			String name = "Save User";
			String password = "Password@123";
			String token = createUserCreationToken(email);

			performPost("/api/v1/auth/register-create-user",
					new CreateUserRequest(name, token, password))
					.andExpect(status().isOk());

			var user = userRepository.findByEmail(email).orElseThrow();
			assert user.getName().equals(name);
			assert user.getEmail().equals(email);
			assert passwordEncoder.matches(password, user.getPassword());
		}

		@Test
		void createUser_invalidToken_returns404() throws Exception {
			performPost("/api/v1/auth/register-create-user",
					new CreateUserRequest("User", "999999", "Password@123"))
					.andExpect(status().isNotFound());
		}

		@Test
		void createUser_blankFields_returns400() throws Exception {
			performPost("/api/v1/auth/register-create-user",
					new CreateUserRequest("", "", ""))
					.andExpect(status().isBadRequest());
		}

		@Test
		void createUser_passwordTooShort_returns400() throws Exception {
			String token = createUserCreationToken("short@example.com");

			performPost("/api/v1/auth/register-create-user",
					new CreateUserRequest("User", token, "short"))
					.andExpect(status().isBadRequest());
		}

		@Test
		void createUser_passwordTooLong_returns400() throws Exception {
			String token = createUserCreationToken("long@example.com");
			String longPassword = "a".repeat(33);

			performPost("/api/v1/auth/register-create-user",
					new CreateUserRequest("User", token, longPassword))
					.andExpect(status().isBadRequest());
		}

		@Test
		void createUser_nameTooLong_returns400() throws Exception {
			String token = createUserCreationToken("longname@example.com");
			String longName = "a".repeat(129);

			performPost("/api/v1/auth/register-create-user",
					new CreateUserRequest(longName, token, "Password@123"))
					.andExpect(status().isBadRequest());
		}
	}

	@Nested
	class Login {

		@Test
		void login_validCredentials_returnsOkAndLoginResponse() throws Exception {
			String email = "login@example.com";
			String password = "Password@123";
			createUserInDb(email, password, "Login User");

			performPost("/api/v1/auth/login", new LoginRequest(email, password))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.id").isNotEmpty())
					.andExpect(jsonPath("$.email").value(email))
					.andExpect(jsonPath("$.role").value("USER"));
		}

		@Test
		void login_wrongPassword_returns401() throws Exception {
			String email = "wrongpass@example.com";
			createUserInDb(email, "Password@123", "Wrong Pass User");

			performPost("/api/v1/auth/login", new LoginRequest(email, "wrongpassword"))
					.andExpect(status().isUnauthorized());
		}

		@Test
		void login_nonExistentUser_returns401() throws Exception {
			performPost("/api/v1/auth/login",
					new LoginRequest("nonexistent@example.com", "Password@123"))
					.andExpect(status().isUnauthorized());
		}

		@Test
		void login_blankEmail_returns400() throws Exception {
			performPost("/api/v1/auth/login", new LoginRequest("", "Password@123"))
					.andExpect(status().isBadRequest());
		}

		@Test
		void login_blankPassword_returns400() throws Exception {
			performPost("/api/v1/auth/login", new LoginRequest("test@example.com", ""))
					.andExpect(status().isBadRequest());
		}
	}

	@Nested
	class RequestResetPassword {

		@Test
		void requestResetPassword_existingUser_returnsOk() throws Exception {
			String email = "resetreq@example.com";
			createUserInDb(email, "Password@123", "Reset User");

			performPost("/api/v1/auth/request-reset-password",
					new RequestResetPasswordRequest(email))
					.andExpect(status().isOk());

			var tokens = authTokenRepository.findActiveVerifications(
					email, AuthTokenType.PASSWORD_RESET_REQUEST, Instant.now());
			assert tokens.size() == 1;
		}

		@Test
		void requestResetPassword_nonExistentUser_returnsOk() throws Exception {
			performPost("/api/v1/auth/request-reset-password",
					new RequestResetPasswordRequest("ghost@example.com"))
					.andExpect(status().isOk());

			var tokens = authTokenRepository.findActiveVerifications(
					"ghost@example.com", AuthTokenType.PASSWORD_RESET_REQUEST, Instant.now());
			assert tokens.isEmpty() : "No token should be created for non-existent user";
		}

		@Test
		void requestResetPassword_calledTwice_revokesOldToken() throws Exception {
			String email = "resetrev@example.com";
			createUserInDb(email, "Password@123", "Reset Rev User");

			performPost("/api/v1/auth/request-reset-password",
					new RequestResetPasswordRequest(email))
					.andExpect(status().isOk());

			performPost("/api/v1/auth/request-reset-password",
					new RequestResetPasswordRequest(email))
					.andExpect(status().isOk());

			var activeTokens = authTokenRepository.findActiveVerifications(
					email, AuthTokenType.PASSWORD_RESET_REQUEST, Instant.now());
			assert activeTokens.size() == 1;
		}

		@Test
		void requestResetPassword_blankEmail_returns400() throws Exception {
			performPost("/api/v1/auth/request-reset-password",
					new RequestResetPasswordRequest(""))
					.andExpect(status().isBadRequest());
		}
	}

	@Nested
	class VerifyResetPassword {

		@Test
		void verifyResetPassword_validToken_returnsOkAndNewToken() throws Exception {
			String email = "verifyreset@example.com";
			String token = createPasswordResetRequestToken(email);

			performPost("/api/v1/auth/verify-reset-password",
					new VerifyPasswordResetRequestRequest(token))
					.andExpect(status().isOk());
		}

		@Test
		void verifyResetPassword_invalidToken_returns404() throws Exception {
			performPost("/api/v1/auth/verify-reset-password",
					new VerifyPasswordResetRequestRequest("999999"))
					.andExpect(status().isNotFound());
		}

		@Test
		void verifyResetPassword_blankToken_returns400() throws Exception {
			performPost("/api/v1/auth/verify-reset-password",
					new VerifyPasswordResetRequestRequest(""))
					.andExpect(status().isBadRequest());
		}

		@Test
		void verifyResetPassword_wrongSizeToken_returns400() throws Exception {
			performPost("/api/v1/auth/verify-reset-password",
					new VerifyPasswordResetRequestRequest("123"))
					.andExpect(status().isBadRequest());
		}
	}

	@Nested
	class ResetPassword {

		@Test
		void resetPassword_validToken_returnsOk() throws Exception {
			String email = "resetpass@example.com";
			String oldPassword = "Password@123";
			String newPassword = "NewPassword@123";
			createUserInDb(email, oldPassword, "Reset Pass User");
			String token = createPasswordResetToken(email);

			performPost("/api/v1/auth/reset-password",
					new ResetPasswordRequest(email, token, newPassword))
					.andExpect(status().isOk());

			// Verify new password works
			performPost("/api/v1/auth/login", new LoginRequest(email, newPassword))
					.andExpect(status().isOk());

			// Verify old password no longer works
			performPost("/api/v1/auth/login", new LoginRequest(email, oldPassword))
					.andExpect(status().isUnauthorized());
		}

		@Test
		void resetPassword_invalidToken_returns404() throws Exception {
			performPost("/api/v1/auth/reset-password",
					new ResetPasswordRequest("test@example.com", "999999", "NewPassword@123"))
					.andExpect(status().isNotFound());
		}

		@Test
		void resetPassword_nonExistentUser_returns404() throws Exception {
			String token = createPasswordResetToken("ghost@example.com");

			performPost("/api/v1/auth/reset-password",
					new ResetPasswordRequest("ghost@example.com", token, "NewPassword@123"))
					.andExpect(status().isNotFound());
		}

		@Test
		void resetPassword_blankFields_returns400() throws Exception {
			performPost("/api/v1/auth/reset-password",
					new ResetPasswordRequest("", "", ""))
					.andExpect(status().isBadRequest());
		}
	}
}
