package com.example.demo.user.adapter.inbound;

import com.example.demo.user.port.inbound.AuthPort;

import java.time.Instant;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
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

import com.example.demo.email.EmailService;
import com.example.demo.user.adapter.outbound.persistence.UserRepository;
import com.example.demo.user.adapter.outbound.persistence.AuthTokenRepository;
import com.example.demo.user.domain.User;
import com.example.demo.user.domain.AuthToken;
import com.example.demo.user.domain.AuthTokenType;
import com.example.demo.user.adapter.web.dto.auth.CreateUserByAuthTokenRequest;
import com.example.demo.user.adapter.web.dto.user.FindMeResponse;
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
import com.example.demo.utils.EnvUtils;
import com.example.demo.utils.HashUtils;
import com.example.demo.utils.TokenService;
import com.example.demo.utils.UuidV7Generator;

import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Service
@Transactional
public class AuthService implements AuthPort {
	private static final Logger log = LoggerFactory.getLogger(AuthService.class);

	private EnvUtils envUtils;
	private PasswordEncoder passwordEncoder;
	private UserRepository userRepository;
	private AuthTokenRepository authTokenRepository;
	private AuthenticationManager authenticationManager;
	private final SecurityContextHolderStrategy securityContextHolderStrategy = SecurityContextHolder
			.getContextHolderStrategy();
	private SecurityContextRepository securityContextRepository = new HttpSessionSecurityContextRepository();
	private EmailService emailService;
	private TokenService tokenService;

	@Value("${AUTH_EMAIL_FROM:test@example.com}")
	private String authEmailFrom;

	public AuthService(
			EnvUtils envUtils,
			PasswordEncoder passwordEncoder,
			UserRepository userRepository,
			AuthTokenRepository authTokenRepository,
			AuthenticationManager authenticationManager,
			TokenService tokenService,
			EmailService emailService) {
		this.envUtils = envUtils;
		this.passwordEncoder = passwordEncoder;
		this.userRepository = userRepository;
		this.authTokenRepository = authTokenRepository;
		this.authenticationManager = authenticationManager;
		this.emailService = emailService;
		this.tokenService = tokenService;
	}

	private static record GenerateAndSaveTokenResponse(UUID authTokenId, String token) {
	}

	private GenerateAndSaveTokenResponse generateAndSaveToken(String email, AuthTokenType type) {
		var token = tokenService.generate();
		var tokenHash = HashUtils.hash(token);

		AuthToken req = new AuthToken(email, tokenHash, type);
		authTokenRepository.save(req);

		return new GenerateAndSaveTokenResponse(req.getId(), token);
	}

	private AuthToken consumeToken(String token, AuthTokenType type) {
		var notFoundException = new EntityNotFoundException("Token not found");

		var foundToken = authTokenRepository
				.findByTokenHashAndType(HashUtils.hash(token), type)
				.orElseThrow(() -> notFoundException);

		if (!foundToken.isValid()) {
			log.info("Token is invalid");
			throw notFoundException;
		}

		foundToken.consume();
		authTokenRepository.save(foundToken);
		log.info(type.toString() + " token consumed");

		return foundToken;
	}

	private void sendTokenToUser(String email, String label, String token) {
		try {
			emailService.sendSimpleMessage(authEmailFrom, email, label, token);
		} catch (Exception e) {
			log.info("Error sending email to email={}: {}", email, e.getMessage(), e);
		}

		if (envUtils.isDevEnvironment()) {
			System.out.println(label + ": " + token);
		}
		log.info(label + " sent to email");
	}

	public RequestEmailVerificationResponse requestEmailVerification(RequestEmailVerificationRequest dto) {
		var type = AuthTokenType.EMAIL_VERIFICATION;
		authTokenRepository.revokeActiveByEmailAndType(dto.email(), type, Instant.now());

		var saveTokenResponse = generateAndSaveToken(dto.email(), type);
		sendTokenToUser(dto.email(), "Email Verification Token", saveTokenResponse.token());

		return new RequestEmailVerificationResponse(saveTokenResponse.authTokenId());
	}

	public VerifyEmailResponse verifyEmail(VerifyEmailRequest dto) throws EntityNotFoundException {
		var oldToken = consumeToken(dto.token(), AuthTokenType.EMAIL_VERIFICATION);
		var saveTokenResponse = generateAndSaveToken(oldToken.getEmail(), AuthTokenType.USER_CREATION);
		return new VerifyEmailResponse(saveTokenResponse.token());
	}

	public LoginResponse createUserByAuthToken(CreateUserByAuthTokenRequest dto, HttpServletRequest request,
			HttpServletResponse response) throws EntityNotFoundException {
		var token = consumeToken(dto.token(), AuthTokenType.USER_CREATION);

		var passwordHash = passwordEncoder.encode(dto.password());
		var user = new User(dto.name(), token.getEmail(), passwordHash);
		userRepository.save(user);

		return login(new LoginRequest(token.getEmail(), dto.password()), request, response);
	}

	public RequestResetPasswordResponse requestResetPassword(RequestResetPasswordRequest dto) {
		var user = userRepository.findByEmail(dto.email());

		if (user.isPresent()) {
			var type = AuthTokenType.PASSWORD_RESET_REQUEST;
			authTokenRepository.revokeActiveByEmailAndType(dto.email(), type, Instant.now());

			var saveTokenResponse = generateAndSaveToken(dto.email(), type);
			sendTokenToUser(dto.email(), "Reset Password Token", saveTokenResponse.token());
			return new RequestResetPasswordResponse(saveTokenResponse.authTokenId());
		}

		return new RequestResetPasswordResponse(UuidV7Generator.generate());
	}

	public VerifyResetPasswordResponse verifyPasswordResetToken(VerifyPasswordResetRequestRequest dto)
			throws EntityNotFoundException {
		var token = consumeToken(dto.token(), AuthTokenType.PASSWORD_RESET_REQUEST);
		return new VerifyResetPasswordResponse(
				generateAndSaveToken(token.getEmail(), AuthTokenType.PASSWORD_RESET).token());
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

	@Transactional(readOnly = true)
	public FindMeResponse findMe(User user) {
		return new FindMeResponse(user.getId(), user.getName(), user.getEmail(), null);
	}

	public void logout(HttpServletRequest request, HttpServletResponse response) {
		SecurityContext context = securityContextHolderStrategy.createEmptyContext();
		securityContextHolderStrategy.setContext(context);
		securityContextRepository.saveContext(context, request, response);
	}
}
