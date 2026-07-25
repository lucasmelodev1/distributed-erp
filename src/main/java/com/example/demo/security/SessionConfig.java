package com.example.demo.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.web.session.HttpSessionEventPublisher;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;
import org.springframework.session.web.http.CookieHttpSessionIdResolver;
import org.springframework.session.web.http.CookieSerializer;
import org.springframework.session.web.http.DefaultCookieSerializer;

@Configuration(proxyBeanMethods = false)
@EnableRedisHttpSession
public class SessionConfig {

	@Bean
	CookieHttpSessionIdResolver httpSessionIdResolver() {
		CookieHttpSessionIdResolver resolver = new CookieHttpSessionIdResolver();
		resolver.setCookieSerializer(cookieSerializer());
		return resolver;
	}

	@Bean
	CookieSerializer cookieSerializer() {
		DefaultCookieSerializer serializer = new DefaultCookieSerializer();
		serializer.setCookieName("JSESSIONID");
		serializer.setCookiePath("/");
		serializer.setUseSecureCookie(true);
		serializer.setUseHttpOnlyCookie(true);
		serializer.setSameSite("Strict");
		return serializer;
	}

	@Bean
	HttpSessionEventPublisher httpSessionEventPublisher() {
		return new HttpSessionEventPublisher();
	}
}
