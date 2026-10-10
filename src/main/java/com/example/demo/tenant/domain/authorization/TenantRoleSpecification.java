package com.example.demo.tenant.domain.authorization;

import java.util.UUID;

import org.jspecify.annotations.Nullable;
import org.springframework.data.jpa.domain.Specification;

public class TenantRoleSpecification {
    public static Specification<TenantRole> hasTenantId(UUID tenantId) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("tenantId"), tenantId);
    }

    public static Specification<TenantRole> hasActiveStatus(@Nullable Boolean active) {
        return (root, query, criteriaBuilder) -> {
            if (active == null) {
                return null;
            }
            return active ? criteriaBuilder.isNull(root.get("inactivatedAt"))
                    : criteriaBuilder.isNotNull(root.get("inactivatedAt"));
        };
    }

    public static Specification<TenantRole> hasName(@Nullable String name) {
        return (root, query, criteriaBuilder) -> name == null ? null
                : criteriaBuilder.like(root.get("name"), "%" + name + "%");
    }
}
