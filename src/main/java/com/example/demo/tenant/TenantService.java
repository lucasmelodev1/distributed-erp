package com.example.demo.tenant;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.tenant.dto.CreateTenantRequest;

@Service
@Transactional
public class TenantService {
	private TenantRepository tenantRepository;

	private TenantUserRepository tenantUserRepository;

	private final AuthenticationManager authenticationManager;

	TenantService(
			AuthenticationManager authenticationManager,
			TenantRepository tenantRepository,
			TenantUserRepository tenantUserRepository) {
		this.authenticationManager = authenticationManager;
		this.tenantUserRepository = tenantUserRepository;
		this.tenantRepository = tenantRepository;
	}

	public void createTenant(CreateTenantRequest dto) {
	}
}
