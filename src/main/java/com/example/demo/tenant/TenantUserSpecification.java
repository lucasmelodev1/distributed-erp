package com.example.demo.tenant;

import java.util.UUID;

import org.jspecify.annotations.Nullable;
import org.springframework.data.jpa.domain.Specification;

public class TenantUserSpecification {
    public static Specification<TenantUser> isActive() {
        return (root, query, criteriaBuilder) -> criteriaBuilder.isNull(root.get("inactivatedAt"));
    }

    public static Specification<TenantUser> hasActiveStatus(@Nullable Boolean active) {
        return (root, query, criteriaBuilder) -> {
            if (active == null) {
                return null;
            }
            if (active) {
                return criteriaBuilder.isNull(root.get("inactivatedAt"));
            } else {
                return criteriaBuilder.isNotNull(root.get("inactivatedAt"));
            }
        };
    }

    public static Specification<TenantUser> hasTenantId(UUID tenantId) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("id").get("tenantId"), tenantId);
    }

    public static Specification<TenantUser> hasUserName(@Nullable String userName) {
        return (root, query, criteriaBuilder) -> {
            if (userName == null || userName.isEmpty()) {
                return criteriaBuilder.conjunction();
            }
            var userJoin = root.join("user");
            return criteriaBuilder.equal(userJoin.get("name"), userName);
        };
    }

}
