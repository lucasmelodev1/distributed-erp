package com.example.demo.product.domain;

import java.util.UUID;

import org.jspecify.annotations.Nullable;
import org.springframework.data.jpa.domain.Specification;

public class ProductSpecification {
	public static Specification<Product> hasTenantId(UUID tenantId) {
		return (root, query, cb) -> cb.equal(root.get("tenantId"), tenantId);
	}

	public static Specification<Product> hasStatus(@Nullable ProductStatus status) {
		return (root, query, cb) -> {
			if (status == null) {
				return null;
			}
			return status == ProductStatus.ACTIVE ? cb.isNull(root.get("inactivatedAt"))
					: cb.isNotNull(root.get("inactivatedAt"));
		};
	}

	/** Name or SKU contains the text; a null or blank text matches everything. */
	public static Specification<Product> matches(@Nullable String text) {
		return (root, query, cb) -> {
			if (text == null || text.isBlank()) {
				return null;
			}
			var pattern = "%" + text.trim() + "%";
			return cb.or(cb.like(root.get("name"), pattern), cb.like(root.get("sku"), pattern));
		};
	}
}
