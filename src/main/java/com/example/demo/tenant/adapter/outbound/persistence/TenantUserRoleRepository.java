package com.example.demo.tenant.adapter.outbound.persistence;
import com.example.demo.tenant.domain.authorization.TenantUserRole;
import com.example.demo.tenant.domain.authorization.TenantUserRoleId;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TenantUserRoleRepository extends JpaRepository<TenantUserRole, TenantUserRoleId> {
}
