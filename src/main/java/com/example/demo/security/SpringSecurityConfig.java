package com.example.demo.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Configuration
@EnableWebSecurity
public class SpringSecurityConfig {

	@Bean
	public PasswordEncoder passwordEncoder() {
		return PasswordEncoderFactories.createDelegatingPasswordEncoder();
	}

	@Bean
	public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
		return config.getAuthenticationManager();
	}

	@Bean
	public AuthenticationEntryPoint authenticationEntryPoint() {
		return (HttpServletRequest request, HttpServletResponse response,
				org.springframework.security.core.AuthenticationException authException) -> {
			response.setStatus(HttpStatus.UNAUTHORIZED.value());
			response.setContentType(MediaType.APPLICATION_JSON_VALUE);
			response.getWriter().write("{\"error\":\"" + authException.getMessage() + "\"}");
		};
	}

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		return http
				.csrf(csrf -> csrf.disable())
				.exceptionHandling(ex -> ex.authenticationEntryPoint(authenticationEntryPoint()))
				.securityContext(context -> context
						.securityContextRepository(new HttpSessionSecurityContextRepository()))
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.ALWAYS))
				.authorizeHttpRequests(auth -> auth
						.requestMatchers("/api/v1/auth/**").permitAll()
						.anyRequest().authenticated())
				.build();
	}

	@Configuration
	@Profile("dev")
	@EnableWebSecurity
	static class DevSecurityConfig {

		@Bean
		SecurityFilterChain devSecurityFilterChain(HttpSecurity http) throws Exception {
			return http
					.securityMatcher("/v3/api-docs/**", "/swagger-ui/**", "/scalar/**")
					.authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
					.build();
		}
	}
}
