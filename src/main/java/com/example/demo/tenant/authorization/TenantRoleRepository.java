package com.example.demo.tenant.authorization;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TenantRoleRepository extends JpaRepository<TenantRole, UUID> {
}
