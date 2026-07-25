package com.example.demo.user;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AuthTokenRepository extends JpaRepository<AuthToken, UUID> {
	public Optional<AuthToken> findByTokenHashAndType(String tokenHash, AuthTokenType type);

	@Query("""
					SELECT e FROM AuthToken e
					WHERE e.email = :email
						AND e.consumedAt IS NULL
						AND e.revokedAt IS NULL
						AND e.expiresAt > :now
						AND e.type = :type
					ORDER BY e.createdAt DESC
			""")
	List<AuthToken> findActiveVerifications(@Param("email") String email, @Param("type") AuthTokenType type,
			@Param("now") Instant now);

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("""
					UPDATE AuthToken e
					SET e.revokedAt = :now
					WHERE e.email = :email
						AND e.consumedAt IS NULL
						AND e.revokedAt IS NULL
						AND e.expiresAt > :now
						AND e.type = :type
			""")
	int revokeActiveByEmailAndType(@Param("email") String email, @Param("type") AuthTokenType type,
			@Param("now") Instant now);
}
