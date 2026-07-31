package com.example.demo.user;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.user.dto.CreateUserByAuthTokenRequest;
import com.example.demo.user.dto.FindMeResponse;
import com.example.demo.user.dto.LoginRequest;
import com.example.demo.user.dto.LoginResponse;
import com.example.demo.user.dto.RequestEmailVerificationRequest;
import com.example.demo.user.dto.RequestEmailVerificationResponse;
import com.example.demo.user.dto.RequestResetPasswordRequest;
import com.example.demo.user.dto.ResetPasswordRequest;
import com.example.demo.user.dto.VerifyEmailRequest;
import com.example.demo.user.dto.VerifyEmailResponse;
import com.example.demo.user.dto.VerifyPasswordResetRequestRequest;
import com.example.demo.user.dto.VerifyResetPasswordResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.GetMapping;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "User login, registration and credential recovery")
public class AuthController {
	private final AuthService authService;

	public AuthController(AuthService authService) {
		this.authService = authService;
	}

	@PostMapping("/login")
	@Operation(summary = "Login", description = "Authenticate a user and return a cookie with the session id")
	public LoginResponse login(
			@Valid @RequestBody LoginRequest dto,
			HttpServletRequest request,
			HttpServletResponse response) {
		return authService.login(dto, request, response);
	}

	@PostMapping("/logout")
	@Operation(summary = "Logout", description = "Invalidate the current session and remove the session cookie")
	public void logout(HttpServletRequest request, HttpServletResponse response) {
		authService.logout(request, response);
	}

	@PostMapping("/register-email-verification")
	@Operation(summary = "Request email verification", description = "The first step in the user registration flow. The user requests a verification token to be sent to their email.")
	public RequestEmailVerificationResponse registerRequestEmailVerification(
			@Valid @RequestBody RequestEmailVerificationRequest dto) {
		return authService.requestEmailVerification(dto);
	}

	@PostMapping("/register-verify-email")
	@Operation(summary = "Verify email", description = "The second step in the user registration flow. The user verifies their email with the sent token and receives a second one to be used to create the account.")
	public VerifyEmailResponse registerVerifyEmail(@Valid @RequestBody VerifyEmailRequest dto) {
		return authService.verifyEmail(dto);
	}

	@PostMapping("/register-create-user")
	@Operation(summary = "Create user", description = "The final step in the user registration flow. The user consumes their verified email token to create and account with the set password and name.")
	public LoginResponse registerCreateUser(
			@Valid @RequestBody CreateUserByAuthTokenRequest dto,
			HttpServletRequest request,
			HttpServletResponse response) {
		return authService.createUserByAuthToken(dto, request, response);
	}

	@PostMapping("/request-reset-password")
	@Operation(summary = "Request password reset", description = "Requests a password reset token to be sent to the user's email. If the email does not exist, the request is silently ignored.")
	public void requestResetPassword(@Valid @RequestBody RequestResetPasswordRequest dto) {
		authService.requestResetPassword(dto);
	}

	@PostMapping("/verify-reset-password")
	@Operation(summary = "Verify password reset token", description = "Consumes a PASSWORD_RESET_REQUEST token and returns a PASSWORD_RESET token to be used to set a new password.")
	public VerifyResetPasswordResponse verifyResetPassword(@Valid @RequestBody VerifyPasswordResetRequestRequest dto) {
		return authService.verifyPasswordResetToken(dto);
	}

	@PostMapping("/reset-password")
	@Operation(summary = "Reset password", description = "Consumes a PASSWORD_RESET token and sets a new password for the user.")
	public void resetPassword(@Valid @RequestBody ResetPasswordRequest dto) {
		authService.resetPassword(dto);
	}
}
