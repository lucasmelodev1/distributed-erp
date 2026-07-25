package com.example.demo.tenant;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TenantRepository extends JpaRepository<Tenant, UUID> {
}
