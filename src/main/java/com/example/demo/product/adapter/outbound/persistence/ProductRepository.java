package com.example.demo.product.adapter.outbound.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.example.demo.product.domain.Product;

public interface ProductRepository extends JpaRepository<Product, UUID>, JpaSpecificationExecutor<Product> {
	Optional<Product> findByIdAndTenantId(UUID id, UUID tenantId);

	boolean existsByTenantIdAndName(UUID tenantId, String name);

	boolean existsByTenantIdAndNameAndIdNot(UUID tenantId, String name, UUID id);

	boolean existsByTenantIdAndSku(UUID tenantId, String sku);

	boolean existsByTenantIdAndSkuAndIdNot(UUID tenantId, String sku, UUID id);
}
