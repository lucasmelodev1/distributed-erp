package com.example.demo.tenant;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TenantUserRepository extends JpaRepository<TenantUser, TenantUserId> {
}
