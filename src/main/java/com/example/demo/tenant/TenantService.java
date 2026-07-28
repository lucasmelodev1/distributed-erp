package com.example.demo.tenant;

import com.example.demo.email.EmailServiceImpl;
import com.example.demo.utils.EnvUtils;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.tenant.authorization.permission.PermissionCheckRepository;
import com.example.demo.tenant.authorization.permission.PermissionRepository;
import com.example.demo.tenant.authorization.tenantrole.TenantRole;
import com.example.demo.tenant.authorization.tenantrole.TenantRoleRepository;
import com.example.demo.tenant.authorization.tenantrolepermission.TenantRolePermission;
import com.example.demo.tenant.authorization.tenantrolepermission.TenantRolePermissionRepository;
import com.example.demo.tenant.authorization.tenantuserrole.TenantUserRole;
import com.example.demo.tenant.authorization.tenantuserrole.TenantUserRoleId;
import com.example.demo.tenant.authorization.tenantuserrole.TenantUserRoleRepository;
import com.example.demo.tenant.dto.CreateTenantRequest;
import com.example.demo.tenant.dto.CreateTenantResponse;
import com.example.demo.tenant.dto.UpdateTenantRequest;
import com.example.demo.tenant.invitation.TenantInvitationRepository;
import com.example.demo.tenant.invitation.TenantInvitationRoleRepository;
import com.example.demo.user.UserRepository;

import jakarta.persistence.EntityNotFoundException;

@Service
public class TenantService {
	private static final Logger log = LoggerFactory.getLogger(TenantService.class);
	private final TenantRepository tenantRepository;
	private final TenantRoleRepository tenantRoleRepository;
	private final TenantUserRepository tenantUserRepository;
	private final TenantUserRoleRepository tenantUserRoleRepository;
	private final TenantRolePermissionRepository tenantRolePermissionRepository;
	private final PermissionRepository permissionRepository;

	@Value("${FRONTEND_URL:http://localhost:3000}")
	private String frontendUrl;

	TenantService(
			TenantRepository tenantRepository,
			TenantUserRepository tenantUserRepository,
			TenantRoleRepository tenantRoleRepository,
			TenantUserRoleRepository tenantUserRoleRepository,
			TenantRolePermissionRepository tenantRolePermissionRepository,
			PermissionCheckRepository permissionCheckRepository,
			PermissionRepository permissionRepository,
			TenantInvitationRepository tenantInvitationRepository,
			TenantInvitationRoleRepository tenantInvitationRoleRepository,
			EmailServiceImpl emailService,
			EnvUtils envUtils,
			UserRepository userRepository) {
		this.tenantUserRepository = tenantUserRepository;
		this.tenantRepository = tenantRepository;
		this.tenantRoleRepository = tenantRoleRepository;
		this.tenantUserRoleRepository = tenantUserRoleRepository;
		this.tenantRolePermissionRepository = tenantRolePermissionRepository;
		this.permissionRepository = permissionRepository;
	}

	@Transactional(readOnly = true)
	public List<Tenant> findAllUserTenants(UUID userId) {
		return tenantRepository.findAllUserTenants(userId);
	}

	@Transactional
	public CreateTenantResponse createTenant(CreateTenantRequest dto, UUID userId) {
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
		var adminRole = new TenantRole(tenant.getId(), "Admin", Set.of());
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

		return new CreateTenantResponse(tenant.getId());
	}

	@Transactional
	public void updateTenant(UpdateTenantRequest dto, UUID tenantId) {
		var tenantQueryResult = tenantRepository.findById(tenantId);
		if (tenantQueryResult.isEmpty()) {
			log.info("Tenant not found. tenant={}", tenantId);
			throw new EntityNotFoundException("Tenant not found");
		}
		var tenant = tenantQueryResult.get();
		tenant.setName(dto.name());
		try {
			tenantRepository.save(tenant);
		} catch (Exception e) {
			log.info("Error updating tenant={} for name={}: {}", tenant.getId(), dto.name(), e.getMessage(), e);
			throw e;
		}
	}

	@Transactional
	public void deleteTenant(UUID tenantId) {
		try {
			tenantRepository.deleteById(tenantId);
		} catch (Exception e) {
			log.info("Error deleting tenant={}: {}", tenantId, e.getMessage(), e);
			throw e;
		}
	}
}
