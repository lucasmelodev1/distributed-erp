package com.example.demo.tenant.adapter.outbound.persistence;
import com.example.demo.tenant.domain.authorization.Permission;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PermissionRepository extends JpaRepository<Permission, String> {
}
