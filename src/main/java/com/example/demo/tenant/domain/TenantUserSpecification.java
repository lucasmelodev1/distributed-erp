package com.example.demo.tenant.domain;

import java.util.UUID;

import org.jspecify.annotations.Nullable;
import com.example.demo.user.domain.User;
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

    /** Keyset condition: members whose user id comes after the given one. */
    public static Specification<TenantUser> isAfterUser(@Nullable UUID userId) {
        return (root, query, criteriaBuilder) -> userId == null
                ? criteriaBuilder.conjunction()
                : criteriaBuilder.greaterThan(root.get("id").<UUID>get("userId"), userId);
    }

    public static Specification<TenantUser> hasTenantId(UUID tenantId) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("id").get("tenantId"), tenantId);
    }

    /** Case-insensitive "name contains" match on the member's user. */
    public static Specification<TenantUser> hasUserName(@Nullable String userName) {
        return (root, query, criteriaBuilder) -> {
            if (userName == null || userName.isBlank()) {
                return criteriaBuilder.conjunction();
            }
            var subquery = query.subquery(Integer.class);
            var user = subquery.from(User.class);
            subquery.select(criteriaBuilder.literal(1)).where(
                    criteriaBuilder.equal(user.get("id"), root.get("id").get("userId")),
                    criteriaBuilder.like(criteriaBuilder.lower(user.get("name")),
                            "%" + userName.toLowerCase(java.util.Locale.ROOT) + "%"));
            return criteriaBuilder.exists(subquery);
        };
    }

}
