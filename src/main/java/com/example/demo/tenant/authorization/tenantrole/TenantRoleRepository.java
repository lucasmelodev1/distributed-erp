package com.example.demo.tenant.authorization.tenantrole;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TenantRoleRepository extends JpaRepository<TenantRole, UUID>, JpaSpecificationExecutor<TenantRole> {
    List<TenantRole> findAllByTenantId(UUID tenantId);

    @Query(value = """
            SELECT tr FROM TenantRole tr
            LEFT JOIN FETCH tr.permissions
            WHERE tr.id = :id
              AND tr.tenantId = :tenantId
            """)
    Optional<TenantRole> findByIdAndTenantId(@Param("id") UUID id, @Param("tenantId") UUID tenantId);
}
