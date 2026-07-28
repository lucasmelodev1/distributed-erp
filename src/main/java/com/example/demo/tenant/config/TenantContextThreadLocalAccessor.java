package com.example.demo.tenant.config;

import java.util.UUID;

import org.springframework.stereotype.Component;

import io.micrometer.context.ThreadLocalAccessor;

@Component
public class TenantContextThreadLocalAccessor implements ThreadLocalAccessor<UUID> {

	@Override
	public Object key() {
		return TenantContext.class.getName();
	}

	@Override
	public UUID getValue() {
		return TenantContext.getTenantId();
	}

	@Override
	public void setValue(UUID value) {
		TenantContext.setTenantId(value);
	}

	@Override
	public void restore() {
		TenantContext.clear();
	}
}
