package com.example.demo.utils;

import java.util.Arrays;

import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class EnvUtils {
	private final Environment env;

	// Constructor injection is the cleanest approach
	public EnvUtils(Environment env) {
		this.env = env;
	}

	public boolean isDevEnvironment() {
		return Arrays.asList(env.getActiveProfiles()).contains("dev");
	}

	public boolean isTestEnvironment() {
		return Arrays.asList(env.getActiveProfiles()).contains("test");
	}
}
