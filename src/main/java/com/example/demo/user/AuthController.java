package com.example.demo.user;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.ResponseStatus;

import com.example.demo.user.dto.CreateUserRequest;
import com.example.demo.user.dto.FindMeResponse;
import com.example.demo.user.dto.LoginRequest;
import com.example.demo.user.dto.LoginResponse;
import com.example.demo.user.dto.RequestEmailVerificationRequest;
import com.example.demo.user.dto.RequestResetPasswordRequest;
import com.example.demo.user.dto.ResetPasswordRequest;
import com.example.demo.user.dto.VerifyEmailRequest;
import com.example.demo.user.dto.VerifyEmailResponse;
import com.example.demo.user.dto.VerifyPasswordResetRequestRequest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityNotFoundException;
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
	private AuthService authService;

	public AuthController(AuthService authService) {
		this.authService = authService;
	}

	@GetMapping("/me")
	@Operation(summary = "Fetch current user details")
	public FindMeResponse findMe(@AuthenticationPrincipal User user) {
		return authService.findMe(user);
	}

	@PostMapping("/login")
	@Operation(summary = "Login", description = "Authenticate a user and return a cookie with the session id")
	public ResponseEntity<LoginResponse> login(
			@Valid @RequestBody LoginRequest dto,
			HttpServletRequest request,
			HttpServletResponse response) {
		return ResponseEntity.ok(authService.login(dto, request, response));
	}

	@PostMapping("/register-email-verification")
	@Operation(summary = "Request email verification", description = "The first step in the user registration flow. The user requests a verification token to be sent to their email.")
	public ResponseEntity<String> registerRequestEmailVerification(
			@Valid @RequestBody RequestEmailVerificationRequest dto) {
		authService.requestEmailVerification(dto);
		return ResponseEntity.ok("Verification token sent via email");
	}

	@PostMapping("/register-verify-email")
	@Operation(summary = "Verify email", description = "The second step in the user registration flow. The user verifies their email with the sent token and receives a second one to be used to create the account.")
	public ResponseEntity<VerifyEmailResponse> registerVerifyEmail(@Valid @RequestBody VerifyEmailRequest dto) {
		var response = authService.verifyEmail(dto);
		return ResponseEntity.ok(response);
	}

	@PostMapping("/register-create-user")
	@Operation(summary = "Create user", description = "The final step in the user registration flow. The user consumes their verified email token to create and account with the set password and name.")
	public ResponseEntity<?> registerCreateUser(@Valid @RequestBody CreateUserRequest dto) {
		authService.createUser(dto);
		return ResponseEntity.ok("User created successfully");
	}

	@PostMapping("/request-reset-password")
	@Operation(summary = "Request password reset", description = "Requests a password reset token to be sent to the user's email. If the email does not exist, the request is silently ignored.")
	public ResponseEntity<String> requestResetPassword(@Valid @RequestBody RequestResetPasswordRequest dto) {
		authService.requestResetPassword(dto);
		return ResponseEntity.ok("If the email exists, a reset token was sent");
	}

	@PostMapping("/verify-reset-password")
	@Operation(summary = "Verify password reset token", description = "Consumes a PASSWORD_RESET_REQUEST token and returns a PASSWORD_RESET token to be used to set a new password.")
	public ResponseEntity<String> verifyResetPassword(@Valid @RequestBody VerifyPasswordResetRequestRequest dto) {
		var token = authService.verifyPasswordResetToken(dto);
		return ResponseEntity.ok(token);
	}

	@PostMapping("/reset-password")
	@Operation(summary = "Reset password", description = "Consumes a PASSWORD_RESET token and sets a new password for the user.")
	public ResponseEntity<String> resetPassword(@Valid @RequestBody ResetPasswordRequest dto) {
		authService.resetPassword(dto);
		return ResponseEntity.ok("Password reset successfully");
	}

	@ExceptionHandler(BadCredentialsException.class)
	@ResponseStatus(HttpStatus.UNAUTHORIZED)
	public String handleBadCredentials(BadCredentialsException ex) {
		return ex.getMessage();
	}

	@ExceptionHandler(EntityNotFoundException.class)
	@ResponseStatus(HttpStatus.NOT_FOUND)
	public String handleNotFound(EntityNotFoundException ex) {
		return ex.getMessage();
	}
}
