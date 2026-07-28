package com.example.demo.tenant.authorization.tenantrolepermission;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TenantRolePermissionRepository
		extends JpaRepository<TenantRolePermission, TenantRolePermissionId> {
}
