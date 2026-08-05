package com.example.demo.utils;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile("test")
public class TokenServiceTestImpl implements TokenService {

	public String generate() {
		return TokenService.testTokenChar.repeat(tokenSize);
	}
}
