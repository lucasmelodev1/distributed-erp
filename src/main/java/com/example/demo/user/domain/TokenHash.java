package com.example.demo.user.domain;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public record TokenHash(String value) {

    public static final int TOKEN_SIZE = 6;

    public static TokenHash of(String plaintext) {
        if (plaintext == null) {
            throw new IllegalArgumentException("Token must not be null");
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(plaintext.getBytes());
            return new TokenHash(bytesToHex(hashBytes));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    public boolean matches(String plaintext) {
        return this.value.equals(of(plaintext).value);
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
