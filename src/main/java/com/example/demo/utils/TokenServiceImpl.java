package com.example.demo.utils;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile("!test")
public class TokenServiceImpl implements TokenService {

	public String generate() {
		try {
			StringBuilder token = new StringBuilder(tokenSize);
			for (int i = 0; i < tokenSize; i++) {
				int randomChar = SecureRandom.getInstanceStrong().nextInt(36);
				if (randomChar < 10) {
					token.append((char) ('0' + randomChar));
				} else {
					token.append((char) ('a' + randomChar - 10));
				}
			}
			return token.toString();
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException("Error generating token", e);
		}
	}
}
