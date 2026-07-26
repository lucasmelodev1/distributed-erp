package com.example.demo.tenant.authorization;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TenantUserRoleRepository extends JpaRepository<TenantUserRole, TenantUserRoleId> {
}
