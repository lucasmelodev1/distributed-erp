package com.example.demo.tenant;

import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.tenant.dto.CreateTenantRequest;
import com.example.demo.tenant.dto.CreateTenantResponse;

@Service
public class TenantService {
	private static final Logger log = LoggerFactory.getLogger(TenantService.class);
	private TenantRepository tenantRepository;

	private TenantUserRepository tenantUserRepository;

	TenantService(
			AuthenticationManager authenticationManager,
			TenantRepository tenantRepository,
			TenantUserRepository tenantUserRepository) {
		this.tenantUserRepository = tenantUserRepository;
		this.tenantRepository = tenantRepository;
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
			log.error("Error creating tenant={}: {}", tenant.getName(), e.getMessage(), e);
			throw e;
		}

		assert (tenant.getId() != null);
		var tenantUser = new TenantUser(new TenantUserId(tenant.getId(), userId));
		try {
			tenantUserRepository.save(tenantUser);
		} catch (Exception e) {
			log.error("Error creating tenant_user with tenant={} and user={}: {}. Reverting tenant creation...",
					tenant.getId(),
					userId,
					e.getMessage(),
					e);
			throw e;
		}

		return new CreateTenantResponse(tenant.getId());
	}
}
