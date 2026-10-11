package com.example.demo.tenant.port.inbound;

import java.util.Optional;
import java.util.UUID;

import org.jspecify.annotations.Nullable;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Window;
import com.example.demo.tenant.domain.authorization.TenantRole;

import com.example.demo.tenant.adapter.web.dto.user.UpdateTenantUserInput;
import com.example.demo.tenant.domain.TenantUser;

public interface TenantUserManagementPort {
    /** Forward keyset page ordered by user id (creation time of the user). */
    Window<TenantUser> scrollTenantUsers(@Nullable String query, @Nullable Boolean active, int limit,
            @Nullable UUID after, UUID tenantId);
    Map<UUID, List<TenantRole>> findRolesByUserIds(Collection<UUID> userIds, UUID tenantId);
    Optional<TenantUser> findTenantUserById(UUID userId, UUID tenantId);
    void updateTenantUserActiveStatus(UUID actorId, UUID userId, boolean active, UUID tenantId);
    /** Removes the membership (soft delete) together with its role assignments. */
    void removeTenantUser(UUID actorId, UUID userId, UUID tenantId);
    void updateTenantUser(UUID actorId, UpdateTenantUserInput dto, UUID tenantId);
}
