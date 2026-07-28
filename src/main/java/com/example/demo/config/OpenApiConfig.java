package com.example.demo.config;

import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.example.demo.tenant.config.TenantAware;

import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.Parameter;

@Configuration
public class OpenApiConfig {

	@Bean
	OperationCustomizer tenantHeaderCustomizer() {
		return (Operation operation, org.springframework.web.method.HandlerMethod handlerMethod) -> {
			if (handlerMethod.hasMethodAnnotation(TenantAware.class)) {
				operation.addParametersItem(new Parameter()
						.name("X-Tenant-Id")
						.in("header")
						.required(true)
						.description("Tenant ID (UUID). The authenticated user must be a member of this tenant.")
						.schema(new StringSchema().format("uuid")));
			}
			return operation;
		};
	}
}
