package com.example.demo.utils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import org.springframework.stereotype.Component;

@Component
public class TokenUtils {

	private static final SecureRandom RANDOM = new SecureRandom();
	private final EnvUtils envUtils;

	private static final int AUTH_TOKEN_DIGITS = 6;

	public TokenUtils(EnvUtils envUtils) {
		this.envUtils = envUtils;
	}

	public static String getTestToken(int digits) {
		return "0".repeat(digits);
	}

	public String generateAuthToken() {
		return generateNumeric(AUTH_TOKEN_DIGITS);
	}

	public static String getAuthTestToken() {
		return getTestToken(AUTH_TOKEN_DIGITS);
	}

	public String generateNumeric(int digits) {
		if (digits <= 0) {
			throw new IllegalArgumentException("digits must be positive");
		}
		int min = (int) Math.pow(10, digits - 1);
		int max = (int) Math.pow(10, digits) - 1;

		if (envUtils.isTestEnvironment()) {
			return getTestToken(digits);
		}

		return String.valueOf(min + RANDOM.nextInt(max - min + 1));
	}

	public String hash(String token) {
		if (token == null) {
			throw new IllegalArgumentException("token must not be null");
		}
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hashBytes = digest.digest(token.getBytes());
			return bytesToHex(hashBytes);
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("SHA-256 not available", e);
		}
	}

	private String bytesToHex(byte[] bytes) {
		StringBuilder sb = new StringBuilder(bytes.length * 2);
		for (byte b : bytes) {
			sb.append(String.format("%02x", b));
		}
		return sb.toString();
	}
}
