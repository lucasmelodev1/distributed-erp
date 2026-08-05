package com.example.demo.tenant.adapter.outbound.persistence;
import com.example.demo.tenant.domain.invitation.TenantInvitationRoleId;
import com.example.demo.tenant.domain.invitation.TenantInvitationRole;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TenantInvitationRoleRepository extends JpaRepository<TenantInvitationRole, TenantInvitationRoleId> {

	List<TenantInvitationRole> findByIdTenantInvitationId(UUID tenantInvitationId);
}
