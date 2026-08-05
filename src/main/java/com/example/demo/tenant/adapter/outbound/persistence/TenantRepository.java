package com.example.demo.tenant.adapter.outbound.persistence;
import com.example.demo.tenant.domain.Tenant;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface TenantRepository extends JpaRepository<Tenant, UUID> {
	@Query(value = """
			SELECT t.* FROM users u
				JOIN tenant_users tu ON tu.user_id = u.id
				JOIN tenants t ON t.id = tu.tenant_id
			WHERE u.id = :userId
				AND tu.deleted_at IS NULL
				AND t.deleted_at IS NULL
				""", nativeQuery = true)
	public List<Tenant> findAllUserTenants(UUID userId);
}
