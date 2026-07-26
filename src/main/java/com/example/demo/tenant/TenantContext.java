package com.example.demo.tenant;

import java.util.UUID;

public final class TenantContext {
	private static final InheritableThreadLocal<UUID> TENANT_ID = new InheritableThreadLocal<>();

	private TenantContext() {
	}

	public static void setTenantId(UUID tenantId) {
		TENANT_ID.set(tenantId);
	}

	public static UUID getTenantId() {
		return TENANT_ID.get();
	}

	public static void clear() {
		TENANT_ID.remove();
	}
}
