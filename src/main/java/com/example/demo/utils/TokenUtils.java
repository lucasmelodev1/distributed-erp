package com.example.demo.utils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

public final class TokenUtils {

	private static final SecureRandom RANDOM = new SecureRandom();

	private TokenUtils() {
	}

	public static String generateNumeric(int digits) {
		if (digits <= 0) {
			throw new IllegalArgumentException("digits must be positive");
		}
		int min = (int) Math.pow(10, digits - 1);
		int max = (int) Math.pow(10, digits) - 1;
		return String.valueOf(min + RANDOM.nextInt(max - min + 1));
	}

	public static String hash(String token) {
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

	private static String bytesToHex(byte[] bytes) {
		StringBuilder sb = new StringBuilder(bytes.length * 2);
		for (byte b : bytes) {
			sb.append(String.format("%02x", b));
		}
		return sb.toString();
	}
}
