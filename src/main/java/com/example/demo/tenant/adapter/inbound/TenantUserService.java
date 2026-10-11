package com.example.demo.tenant.adapter.inbound;

import com.example.demo.tenant.port.inbound.TenantUserManagementPort;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.jspecify.annotations.Nullable;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.ScrollPosition;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Window;
import com.example.demo.tenant.adapter.outbound.persistence.TenantUserRoleRepository.UserRoleRow;
import com.example.demo.tenant.domain.authorization.TenantRole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.tenant.adapter.outbound.persistence.TenantUserRoleRepository;
import com.example.demo.tenant.adapter.web.dto.user.UpdateTenantUserInput;
import com.example.demo.tenant.domain.TenantUser;
import com.example.demo.tenant.domain.TenantUserId;
import com.example.demo.tenant.domain.TenantUserSpecification;
import com.example.demo.tenant.adapter.outbound.persistence.TenantUserRepository;

import jakarta.persistence.EntityNotFoundException;

@Service
public class TenantUserService implements TenantUserManagementPort {
    private static final Logger log = LoggerFactory.getLogger(TenantUserService.class);
    private final TenantUserRepository tenantUserRepository;
    private final TenantUserRoleRepository tenantUserRoleRepository;
    private final TenantRoleChecks roleChecks;

    TenantUserService(
            TenantUserRepository tenantUserRepository,
            TenantUserRoleRepository tenantUserRoleRepository,
            TenantRoleChecks roleChecks) {
        this.tenantUserRepository = tenantUserRepository;
        this.tenantUserRoleRepository = tenantUserRoleRepository;
        this.roleChecks = roleChecks;
    }

    @Transactional(readOnly = true)
    public Window<TenantUser> scrollTenantUsers(@Nullable String query, @Nullable Boolean active, int limit,
            @Nullable UUID after, UUID tenantId) {
        var spec = Specification.allOf(
                TenantUserSpecification.hasActiveStatus(active),
                TenantUserSpecification.hasTenantId(tenantId),
                TenantUserSpecification.hasUserName(query));
        var rows = tenantUserRepository.findBy(
                spec.and(TenantUserSpecification.isAfterUser(after)),
                q -> q.sortBy(Sort.by("id.userId")).limit(limit + 1).all());
        boolean hasNext = rows.size() > limit;
        return Window.from(new java.util.ArrayList<>(hasNext ? rows.subList(0, limit) : rows),
                ScrollPosition::offset, hasNext);
    }

    @Transactional(readOnly = true)
    public Map<UUID, List<TenantRole>> findRolesByUserIds(Collection<UUID> userIds, UUID tenantId) {
        if (userIds.isEmpty()) {
            return Map.of();
        }
        return tenantUserRoleRepository.findRolesByUserIds(userIds, tenantId).stream()
                .collect(Collectors.groupingBy(UserRoleRow::userId,
                        Collectors.mapping(UserRoleRow::role, Collectors.toList())));
    }

    @Transactional(readOnly = true)
    public Optional<TenantUser> findTenantUserById(UUID userId, UUID tenantId) {
        return tenantUserRepository.findById(new TenantUserId(tenantId, userId));
    }

    @Transactional
    public void updateTenantUserActiveStatus(UUID actorId, UUID userId, boolean active, UUID tenantId) {
        if (!active && actorId.equals(userId)) {
            throw new IllegalArgumentException("You cannot inactivate yourself");
        }
        var tenantUserId = new TenantUserId(tenantId, userId);
        var tenantUserQueryResult = tenantUserRepository.findById(tenantUserId);
        if (tenantUserQueryResult.isEmpty()) {
            log.info("Tenant user not found for tenant={} and user={}", tenantId, userId);
            throw new EntityNotFoundException("Tenant user not found");
        }

        var tenantUser = tenantUserQueryResult.get();
        if (active) {
            log.info("Reactivating tenant user for tenant={} and user={}", tenantId, userId);
            tenantUser.reactivate();
        } else {
            log.info("Inactivating tenant user for tenant={} and user={}", tenantId, userId);
            tenantUser.inactivate();
        }
        tenantUserRepository.save(tenantUser);

        log.info("Inactivated tenant user for tenant={} and user={}", tenantId, userId);
    }

    @Transactional
    public void removeTenantUser(UUID actorId, UUID userId, UUID tenantId) {
        if (actorId.equals(userId)) {
            throw new IllegalArgumentException("You cannot remove yourself");
        }
        var tenantUser = tenantUserRepository.findById(new TenantUserId(tenantId, userId))
                .orElseThrow(() -> new EntityNotFoundException("Tenant user not found"));
        // Soft delete; the cascade also removes the role assignments, so a returning member starts clean.
        tenantUserRepository.delete(tenantUser);
        log.info("Removed user={} from tenant={}", userId, tenantId);
    }

    @Transactional
    public void updateTenantUser(UUID actorId, UpdateTenantUserInput dto, UUID tenantId) {
        var userId = dto.userId();
        if (actorId.equals(userId)) {
            throw new IllegalArgumentException("You cannot change your own roles");
        }
        var tenantUserQueryResult = tenantUserRepository.findById(new TenantUserId(tenantId, userId));
        if (tenantUserQueryResult.isEmpty()) {
            log.info("Tenant user not found for tenant={} and user={}", tenantId, userId);
            throw new EntityNotFoundException("Tenant user not found");
        }

        var tenantUser = tenantUserQueryResult.get();
        roleChecks.requireRolesOfTenant(dto.tenantRoleIds(), tenantId);
        // Only roles the member does not hold yet are checked: keeping an inactive role is fine, adding one is not.
        var held = tenantUser.getRoles() == null ? java.util.Set.<UUID>of()
                : tenantUser.getRoles().stream().map(role -> role.getId().tenantRoleId()).collect(Collectors.toSet());
        var added = dto.tenantRoleIds().stream().filter(id -> !held.contains(id)).distinct().toList();
        roleChecks.requireActiveRolesOfTenant(added, tenantId);
        roleChecks.requireGrantable(added, actorId, tenantId);
        tenantUser.replaceRoles(dto.tenantRoleIds());

        try {
            tenantUserRepository.save(tenantUser);
        } catch (Exception e) {
            log.warn("Error updating tenant user roles for tenant={} and user={}: {}",
                    tenantId,
                    userId,
                    e.getMessage(),
                    e);
            throw e;
        }

        log.debug("Updated tenant user for tenant={} and user={} successfully", tenantId, userId);
    }
}
