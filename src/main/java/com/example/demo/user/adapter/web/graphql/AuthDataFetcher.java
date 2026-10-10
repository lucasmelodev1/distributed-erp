package com.example.demo.user.adapter.web.graphql;

import java.util.Map;

import com.example.demo.config.graphql.GraphQlInputValidator;
import com.example.demo.config.graphql.GraphQlRequestContext;
import com.example.demo.config.graphql.PublicOperation;
import com.example.demo.user.adapter.web.dto.auth.CreateUserByAuthTokenRequest;
import com.example.demo.user.adapter.web.dto.auth.LoginRequest;
import com.example.demo.user.adapter.web.dto.auth.LoginResponse;
import com.example.demo.user.adapter.web.dto.auth.RequestEmailVerificationRequest;
import com.example.demo.user.adapter.web.dto.auth.RequestEmailVerificationResponse;
import com.example.demo.user.adapter.web.dto.auth.RequestResetPasswordRequest;
import com.example.demo.user.adapter.web.dto.auth.RequestResetPasswordResponse;
import com.example.demo.user.adapter.web.dto.auth.ResetPasswordRequest;
import com.example.demo.user.adapter.web.dto.auth.VerifyEmailRequest;
import com.example.demo.user.adapter.web.dto.auth.VerifyEmailResponse;
import com.example.demo.user.adapter.web.dto.auth.VerifyPasswordResetRequestRequest;
import com.example.demo.user.adapter.web.dto.auth.VerifyResetPasswordResponse;
import com.example.demo.user.adapter.web.dto.user.FindMeResponse;
import com.example.demo.user.port.inbound.AuthPort;
import com.example.demo.user.port.inbound.UserProfilePort;
import com.netflix.graphql.dgs.DgsComponent;
import com.netflix.graphql.dgs.DgsDataFetchingEnvironment;
import com.netflix.graphql.dgs.DgsMutation;
import com.netflix.graphql.dgs.DgsQuery;
import com.netflix.graphql.dgs.InputArgument;

@DgsComponent
public class AuthDataFetcher {
	private final AuthPort authService;
	private final UserProfilePort userService;
	private final GraphQlInputValidator validator;

	public AuthDataFetcher(AuthPort authService, UserProfilePort userService, GraphQlInputValidator validator) {
		this.authService = authService;
		this.userService = userService;
		this.validator = validator;
	}

	@DgsQuery
	public FindMeResponse me(DgsDataFetchingEnvironment dfe) {
		return userService.findMe(GraphQlRequestContext.currentUser(dfe));
	}

	@PublicOperation
	@DgsMutation
	public RequestEmailVerificationResponse requestEmailVerification(
			@InputArgument RequestEmailVerificationRequest input) {
		return authService.requestEmailVerification(validator.validate(input));
	}

	@PublicOperation
	@DgsMutation
	public VerifyEmailResponse verifyEmail(@InputArgument VerifyEmailRequest input) {
		return authService.verifyEmail(validator.validate(input));
	}

	@PublicOperation
	@DgsMutation
	public LoginResponse registerUser(@InputArgument("input") CreateUserByAuthTokenRequest input,
			DgsDataFetchingEnvironment dfe) {
		return authService.createUserByAuthToken(validator.validate(input),
				GraphQlRequestContext.httpRequest(dfe), GraphQlRequestContext.httpResponse(dfe));
	}

	@PublicOperation
	@DgsMutation
	public LoginResponse login(@InputArgument LoginRequest input, DgsDataFetchingEnvironment dfe) {
		return authService.login(validator.validate(input),
				GraphQlRequestContext.httpRequest(dfe), GraphQlRequestContext.httpResponse(dfe));
	}

	@DgsMutation
	public Map<String, Object> logout(DgsDataFetchingEnvironment dfe) {
		authService.logout(GraphQlRequestContext.httpRequest(dfe), GraphQlRequestContext.httpResponse(dfe));
		return Map.of("success", true);
	}

	@PublicOperation
	@DgsMutation
	public RequestResetPasswordResponse requestPasswordReset(@InputArgument RequestResetPasswordRequest input) {
		return authService.requestResetPassword(validator.validate(input));
	}

	@PublicOperation
	@DgsMutation
	public VerifyResetPasswordResponse verifyPasswordReset(@InputArgument VerifyPasswordResetRequestRequest input) {
		return authService.verifyPasswordResetToken(validator.validate(input));
	}

	@PublicOperation
	@DgsMutation
	public Map<String, Object> resetPassword(@InputArgument ResetPasswordRequest input) {
		authService.resetPassword(validator.validate(input));
		return Map.of("success", true);
	}
}
