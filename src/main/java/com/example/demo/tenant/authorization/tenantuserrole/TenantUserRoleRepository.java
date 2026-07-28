package com.example.demo.tenant.authorization.tenantuserrole;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TenantUserRoleRepository extends JpaRepository<TenantUserRole, TenantUserRoleId> {
}
