package com.example.demo.tenant.invitation;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TenantInvitationRoleRepository extends JpaRepository<TenantInvitationRole, TenantInvitationRoleId> {

	List<TenantInvitationRole> findByIdTenantInvitationId(UUID tenantInvitationId);
}
