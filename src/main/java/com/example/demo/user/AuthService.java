package com.example.demo.user;

import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
import com.example.demo.utils.EnvUtils;
import com.example.demo.utils.TokenUtils;

import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Service
@Transactional
public class AuthService {
	private static final Logger log = LoggerFactory.getLogger(AuthService.class);

	private EnvUtils envUtils;
	private PasswordEncoder passwordEncoder;
	private UserRepository userRepository;
	private AuthTokenRepository authTokenRepository;
	private AuthenticationManager authenticationManager;
	private final SecurityContextHolderStrategy securityContextHolderStrategy = SecurityContextHolder
			.getContextHolderStrategy();
	private SecurityContextRepository securityContextRepository = new HttpSessionSecurityContextRepository();

	public AuthService(EnvUtils envUtils, PasswordEncoder passwordEncoder, UserRepository userRepository,
			AuthTokenRepository authTokenRepository, AuthenticationManager authenticationManager) {
		this.envUtils = envUtils;
		this.passwordEncoder = passwordEncoder;
		this.userRepository = userRepository;
		this.authTokenRepository = authTokenRepository;
		this.authenticationManager = authenticationManager;
	}

	private String generateAndSaveToken(String email, AuthTokenType type) {
		var token = TokenUtils.generateNumeric(AuthToken.TOKEN_SIZE);
		var tokenHash = TokenUtils.hash(token);

		AuthToken req = new AuthToken(email, tokenHash, type);
		authTokenRepository.save(req);

		return token;
	}

	private AuthToken consumeToken(String token, AuthTokenType type) {
		var notFoundException = new EntityNotFoundException("Token not found");

		var foundToken = authTokenRepository
				.findByTokenHashAndType(TokenUtils.hash(token), type)
				.orElseThrow(() -> notFoundException);

		if (!foundToken.isValid()) {
			log.warn("Token is invalid");
			throw notFoundException;
		}

		foundToken.consume();
		authTokenRepository.save(foundToken);
		log.info(type.toString() + " token consumed");

		return foundToken;
	}

	private void logTokenInDev(String label, String token) {
		// TODO: send email
		if (envUtils.isDevEnvironment()) {
			System.out.println(label + ": " + token);
		}
		log.info(label + " sent to email");
	}

	public void requestEmailVerification(RequestEmailVerificationRequest dto) {
		var type = AuthTokenType.EMAIL_VERIFICATION;
		authTokenRepository.revokeActiveByEmailAndType(dto.email(), type, Instant.now());

		var token = generateAndSaveToken(dto.email(), type);
		logTokenInDev("Email Verification Token", token);
	}

	public VerifyEmailResponse verifyEmail(VerifyEmailRequest dto) throws EntityNotFoundException {
		var oldToken = consumeToken(dto.token(), AuthTokenType.EMAIL_VERIFICATION);
		var newToken = generateAndSaveToken(oldToken.getEmail(), AuthTokenType.USER_CREATION);
		return new VerifyEmailResponse(newToken);
	}

	public void createUser(CreateUserRequest dto) throws EntityNotFoundException {
		var token = consumeToken(dto.token(), AuthTokenType.USER_CREATION);

		var passwordHash = passwordEncoder.encode(dto.password());
		var user = new User(dto.name(), token.getEmail(), passwordHash);
		userRepository.save(user);
	}

	public void requestResetPassword(RequestResetPasswordRequest dto) {
		var user = userRepository.findByEmail(dto.email());

		if (user.isPresent()) {
			var type = AuthTokenType.PASSWORD_RESET_REQUEST;
			authTokenRepository.revokeActiveByEmailAndType(dto.email(), type, Instant.now());

			var token = generateAndSaveToken(dto.email(), type);
			logTokenInDev("Reset Password Token", token);
		}
	}

	public String verifyPasswordResetToken(VerifyPasswordResetRequestRequest dto) throws EntityNotFoundException {
		var token = consumeToken(dto.token(), AuthTokenType.PASSWORD_RESET_REQUEST);
		return generateAndSaveToken(token.getEmail(), AuthTokenType.PASSWORD_RESET);
	}

	public void resetPassword(ResetPasswordRequest dto) throws EntityNotFoundException {
		consumeToken(dto.token(), AuthTokenType.PASSWORD_RESET);

		var passwordHash = passwordEncoder.encode(dto.password());
		var user = userRepository
				.findByEmail(dto.email())
				.orElseThrow(() -> new EntityNotFoundException("Error resetting password"));

		user.setPassword(passwordHash);
		userRepository.save(user);
	}

	public LoginResponse login(LoginRequest dto, HttpServletRequest request, HttpServletResponse response)
			throws BadCredentialsException {
		Authentication auth = authenticationManager
				.authenticate(new UsernamePasswordAuthenticationToken(dto.email(), dto.password()));
		SecurityContext context = securityContextHolderStrategy.createEmptyContext();
		context.setAuthentication(auth);
		securityContextHolderStrategy.setContext(context);
		securityContextRepository.saveContext(context, request, response);
		User user = (User) auth.getPrincipal();
		log.info("User " + user.getId() + " logged in");
		return new LoginResponse(user.getId(), user.getEmail(), user.getRole());
	}

	public FindMeResponse findMe(User user) {
		return new FindMeResponse(user.getId(), user.getName(), user.getEmail(), user.getProfileIconUrl());
	}
}
