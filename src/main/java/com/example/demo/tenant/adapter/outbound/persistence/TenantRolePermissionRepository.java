package com.example.demo.tenant.adapter.outbound.persistence;
import com.example.demo.tenant.domain.authorization.TenantRolePermission;
import com.example.demo.tenant.domain.authorization.TenantRolePermissionId;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TenantRolePermissionRepository
		extends JpaRepository<TenantRolePermission, TenantRolePermissionId> {
}
