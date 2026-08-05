package com.example.demo.tenant.adapter.outbound.persistence;
import com.example.demo.tenant.domain.TenantUser;
import com.example.demo.tenant.domain.TenantUserId;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TenantUserRepository
		extends
		JpaRepository<TenantUser, TenantUserId>,
		JpaSpecificationExecutor<TenantUser> {
	@Query(value = """
			SELECT tu FROM User u
			JOIN TenantUser tu ON tu.id.userId = u.id
			WHERE u.email = :email
				AND tu.id.tenantId = :tenantId
				""")
	List<TenantUser> findByUserEmailAndTenantId(@Param("email") String email, @Param("tenantId") UUID tenantId);
}
