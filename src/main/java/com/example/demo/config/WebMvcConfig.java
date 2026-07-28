package com.example.demo.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.example.demo.tenant.config.TenantValidationInterceptor;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
	private final TenantValidationInterceptor tenantValidationInterceptor;

	public WebMvcConfig(TenantValidationInterceptor tenantValidationInterceptor) {
		this.tenantValidationInterceptor = tenantValidationInterceptor;
	}

	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		registry.addInterceptor(tenantValidationInterceptor)
				.addPathPatterns("/api/v1/**");
	}
}
