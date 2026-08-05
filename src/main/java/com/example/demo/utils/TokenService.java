package com.example.demo.utils;

public interface TokenService {
	public String testTokenChar = "A";
	public static final int tokenSize = 6;

	/**
	 * Generates a random alphanumeric token of the specified size.
	 * 
	 * @param size the size of the token to generate
	 * @return a random alphanumeric token
	 */
	public String generate();
}
