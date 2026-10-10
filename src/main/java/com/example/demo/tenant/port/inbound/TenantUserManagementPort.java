package com.example.demo.tenant.port.inbound;

import java.util.Optional;
import java.util.UUID;

import org.jspecify.annotations.Nullable;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Window;
import com.example.demo.tenant.domain.authorization.TenantRole;
import org.springframework.data.domain.Pageable;

import com.example.demo.tenant.adapter.web.dto.user.InactivateTenantUserRequest;
import com.example.demo.tenant.adapter.web.dto.user.UpdateTenantUserRequest;
import com.example.demo.tenant.domain.TenantUser;

public interface TenantUserManagementPort {
    Page<TenantUser> findAllTenantUsers(@Nullable String query, @Nullable Boolean active, Pageable pageable, UUID tenantId);
    /** Forward keyset page ordered by user id (creation time of the user). */
    Window<TenantUser> scrollTenantUsers(@Nullable String query, @Nullable Boolean active, int limit,
            @Nullable UUID after, UUID tenantId);
    Map<UUID, List<TenantRole>> findRolesByUserIds(Collection<UUID> userIds, UUID tenantId);
    Optional<TenantUser> findTenantUserById(UUID userId, UUID tenantId);
    void updateTenantUserActiveStatus(UUID actorId, UUID userId, InactivateTenantUserRequest dto, UUID tenantId);
    /** Removes the membership (soft delete) together with its role assignments. */
    void removeTenantUser(UUID actorId, UUID userId, UUID tenantId);
    void updateTenantUser(UUID userId, UpdateTenantUserRequest dto, UUID tenantId);
}
