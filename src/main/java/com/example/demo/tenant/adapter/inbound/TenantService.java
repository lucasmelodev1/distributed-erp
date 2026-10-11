package com.example.demo.tenant.adapter.inbound;

import com.example.demo.tenant.port.inbound.TenantManagementPort;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.tenant.adapter.outbound.persistence.PermissionRepository;
import com.example.demo.tenant.domain.authorization.TenantRole;
import com.example.demo.tenant.adapter.outbound.persistence.TenantRoleRepository;
import com.example.demo.tenant.domain.authorization.TenantRolePermission;
import com.example.demo.tenant.adapter.outbound.persistence.TenantRolePermissionRepository;
import com.example.demo.tenant.domain.authorization.TenantUserRole;
import com.example.demo.tenant.domain.authorization.TenantUserRoleId;
import com.example.demo.tenant.adapter.outbound.persistence.TenantUserRoleRepository;
import com.example.demo.tenant.adapter.web.dto.tenant.CreateTenantInput;
import com.example.demo.tenant.adapter.web.dto.tenant.UpdateTenantInput;
import com.example.demo.tenant.config.AccessCache;
import com.example.demo.tenant.domain.Tenant;
import com.example.demo.tenant.domain.TenantUser;
import com.example.demo.tenant.domain.TenantUserId;
import com.example.demo.tenant.adapter.outbound.persistence.TenantRepository;
import com.example.demo.tenant.adapter.outbound.persistence.TenantUserRepository;

import jakarta.persistence.EntityNotFoundException;

@Service
public class TenantService implements TenantManagementPort {
	private static final Logger log = LoggerFactory.getLogger(TenantService.class);
	private final TenantRepository tenantRepository;
	private final TenantRoleRepository tenantRoleRepository;
	private final TenantUserRepository tenantUserRepository;
	private final TenantUserRoleRepository tenantUserRoleRepository;
	private final TenantRolePermissionRepository tenantRolePermissionRepository;
	private final PermissionRepository permissionRepository;
	private final AccessCache accessCache;

	@Value("${FRONTEND_URL:http://localhost:5173}")
	private String frontendUrl;

	TenantService(
			TenantRepository tenantRepository,
			TenantUserRepository tenantUserRepository,
			TenantRoleRepository tenantRoleRepository,
			TenantUserRoleRepository tenantUserRoleRepository,
			TenantRolePermissionRepository tenantRolePermissionRepository,
			PermissionRepository permissionRepository,
			AccessCache accessCache) {
		this.tenantUserRepository = tenantUserRepository;
		this.tenantRepository = tenantRepository;
		this.tenantRoleRepository = tenantRoleRepository;
		this.tenantUserRoleRepository = tenantUserRoleRepository;
		this.tenantRolePermissionRepository = tenantRolePermissionRepository;
		this.permissionRepository = permissionRepository;
		this.accessCache = accessCache;
	}

	@Transactional(readOnly = true)
	public List<Tenant> findAllUserTenants(UUID userId) {
		return tenantRepository.findAllUserTenants(userId);
	}

	@Transactional
	public Tenant createTenant(CreateTenantInput dto, UUID userId) {
		var tenant = new Tenant(dto.name());
		try {
			tenantRepository.save(tenant);
		} catch (Exception e) {
			log.info("Error creating tenant={}: {}", tenant.getName(), e.getMessage(), e);
			throw e;
		}

		assert (tenant.getId() != null);
		var tenantUser = new TenantUser(new TenantUserId(tenant.getId(), userId));
		try {
			tenantUserRepository.save(tenantUser);
		} catch (Exception e) {
			log.info("Error creating tenant_user with tenant={} and user={}: {}. Reverting tenant creation...",
					tenant.getId(),
					userId,
					e.getMessage(),
					e);
			throw e;
		}

		// Create admin role for this tenant
		var adminRole = TenantRole.builtIn(tenant.getId(), "Admin", Set.of());
		try {
			tenantRoleRepository.save(adminRole);
		} catch (Exception e) {
			log.info("Error creating admin role for tenant={}: {}", tenant.getId(), e.getMessage(), e);
			throw e;
		}

		// Add all available permissions
		List<TenantRolePermission> adminRolePermissions = permissionRepository
				.findAll()
				.stream()
				.map(permission -> {
					var p = new TenantRolePermission(adminRole, permission.getCode(), tenant.getId());
					return p;
				})
				.toList();
		try {
			tenantRolePermissionRepository.saveAll(adminRolePermissions);
		} catch (Exception e) {
			log.info("Error saving all roles for admin role={} in tenant={}: {}",
					adminRole.getId(),
					tenant.getId(),
					e.getMessage(),
					e);
			throw e;
		}

		// Associate admin role to the user
		var tenantUserRole = new TenantUserRole(new TenantUserRoleId(userId, tenant.getId(), adminRole.getId()));
		try {
			tenantUserRoleRepository.save(tenantUserRole);
		} catch (Exception e) {
			log.info("Could not associate role admin role={} with user={} in tenant={}: {}",
					adminRole.getId(),
					userId,
					tenant.getId(),
					e.getMessage(),
					e);
			throw e;
		}

		accessCache.invalidateUser(userId);
		return tenant;
	}

	@Transactional
	public Tenant updateTenant(UpdateTenantInput dto, UUID tenantId) {
		var tenantQueryResult = tenantRepository.findById(tenantId);
		if (tenantQueryResult.isEmpty()) {
			log.info("Tenant not found. tenant={}", tenantId);
			throw new EntityNotFoundException("Tenant not found");
		}
		var tenant = tenantQueryResult.get();
		tenant.setName(dto.name());
		try {
			return tenantRepository.save(tenant);
		} catch (Exception e) {
			log.info("Error updating tenant={} for name={}: {}", tenant.getId(), dto.name(), e.getMessage(), e);
			throw e;
		}
	}

	@Transactional
	public Tenant setTenantActive(UUID tenantId, boolean active) {
		var tenant = tenantRepository.findById(tenantId)
				.orElseThrow(() -> new EntityNotFoundException("Tenant not found"));
		if (active) {
			tenant.reactivate();
		} else {
			tenant.inactivate();
		}
		log.info("Tenant={} is now {}", tenantId, active ? "active" : "inactive");
		accessCache.invalidateTenant(tenantId);
		return tenantRepository.save(tenant);
	}

	@Transactional(readOnly = true)
	public Optional<Tenant> findTenantById(UUID tenantId) {
		return tenantRepository.findById(tenantId);
	}
}
