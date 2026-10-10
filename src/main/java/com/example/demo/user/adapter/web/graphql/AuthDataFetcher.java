package com.example.demo.user.adapter.web.graphql;

import java.util.Map;

import com.example.demo.config.graphql.GraphQlInputValidator;
import com.example.demo.config.graphql.GraphQlRequestContext;
import com.example.demo.config.graphql.PublicOperation;
import com.example.demo.user.adapter.web.dto.auth.RegisterUserInput;
import com.example.demo.user.adapter.web.dto.auth.LoginInput;
import com.example.demo.user.adapter.web.dto.auth.LoginPayload;
import com.example.demo.user.adapter.web.dto.auth.RequestEmailVerificationInput;
import com.example.demo.user.adapter.web.dto.auth.RequestEmailVerificationPayload;
import com.example.demo.user.adapter.web.dto.auth.RequestPasswordResetInput;
import com.example.demo.user.adapter.web.dto.auth.RequestPasswordResetPayload;
import com.example.demo.user.adapter.web.dto.auth.ResetPasswordInput;
import com.example.demo.user.adapter.web.dto.auth.VerifyEmailInput;
import com.example.demo.user.adapter.web.dto.auth.VerifyEmailPayload;
import com.example.demo.user.adapter.web.dto.auth.VerifyPasswordResetInput;
import com.example.demo.user.adapter.web.dto.auth.VerifyPasswordResetPayload;
import com.example.demo.user.port.inbound.AuthPort;
import com.netflix.graphql.dgs.DgsComponent;
import com.netflix.graphql.dgs.DgsDataFetchingEnvironment;
import com.netflix.graphql.dgs.DgsMutation;
import com.netflix.graphql.dgs.InputArgument;

@DgsComponent
public class AuthDataFetcher {
	private final AuthPort authService;
	private final GraphQlInputValidator validator;

	public AuthDataFetcher(AuthPort authService, GraphQlInputValidator validator) {
		this.authService = authService;
		this.validator = validator;
	}

	@PublicOperation
	@DgsMutation
	public RequestEmailVerificationPayload requestEmailVerification(
			@InputArgument RequestEmailVerificationInput input) {
		return authService.requestEmailVerification(validator.validate(input));
	}

	@PublicOperation
	@DgsMutation
	public VerifyEmailPayload verifyEmail(@InputArgument VerifyEmailInput input) {
		return authService.verifyEmail(validator.validate(input));
	}

	@PublicOperation
	@DgsMutation
	public LoginPayload registerUser(@InputArgument("input") RegisterUserInput input,
			DgsDataFetchingEnvironment dfe) {
		return authService.createUserByAuthToken(validator.validate(input),
				GraphQlRequestContext.httpRequest(dfe), GraphQlRequestContext.httpResponse(dfe));
	}

	@PublicOperation
	@DgsMutation
	public LoginPayload login(@InputArgument LoginInput input, DgsDataFetchingEnvironment dfe) {
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
	public RequestPasswordResetPayload requestPasswordReset(@InputArgument RequestPasswordResetInput input) {
		return authService.requestResetPassword(validator.validate(input));
	}

	@PublicOperation
	@DgsMutation
	public VerifyPasswordResetPayload verifyPasswordReset(@InputArgument VerifyPasswordResetInput input) {
		return authService.verifyPasswordResetToken(validator.validate(input));
	}

	@PublicOperation
	@DgsMutation
	public Map<String, Object> resetPassword(@InputArgument ResetPasswordInput input) {
		authService.resetPassword(validator.validate(input));
		return Map.of("success", true);
	}
}
