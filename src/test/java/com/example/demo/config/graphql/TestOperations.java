package com.example.demo.config.graphql;

import java.time.Instant;
import java.util.UUID;

import com.example.demo.tenant.config.TenantAware;
import com.example.demo.tenant.config.TenantContext;
import com.netflix.graphql.dgs.DgsComponent;
import com.netflix.graphql.dgs.DgsQuery;
import com.netflix.graphql.dgs.InputArgument;

import jakarta.persistence.EntityNotFoundException;

/** Test-only operations used to exercise the GraphQL infrastructure. */
@DgsComponent
class TestOperations {
	record TestNode(String value) {
		TestNode child() {
			return new TestNode(value);
		}
	}

	@DgsQuery
	public String testAuthenticated() {
		return "ok";
	}

	@TenantAware
	@DgsQuery
	public String testTenant() {
		return "ok";
	}

	@TenantAware
	@DgsQuery
	public String testCurrentTenant() {
		return TenantContext.getTenantId().toString();
	}

	@TenantAware(withPermissions = "tenants:update")
	@DgsQuery
	public String testTenantWithPermission() {
		return "ok";
	}

	@TenantAware(withPermissions = "tests:does-not-exist")
	@DgsQuery
	public String testTenantMissingPermission() {
		return "ok";
	}

	@PublicOperation
	@DgsQuery
	public TestNode testNode() {
		return new TestNode("leaf");
	}

	@PublicOperation
	@DgsQuery
	public UUID testUuid(@InputArgument UUID value) {
		return value;
	}

	@PublicOperation
	@DgsQuery
	public Instant testNow() {
		return Instant.parse("2026-10-10T12:00:00Z");
	}

	@PublicOperation
	@DgsQuery
	public String testThrowsUnexpected() {
		throw new IllegalStateException("secret internal detail");
	}

	@PublicOperation
	@DgsQuery
	public String testThrowsNotFound() {
		throw new EntityNotFoundException("Thing not found");
	}

	@PublicOperation
	@DgsQuery
	public String testThrowsIllegalArgument() {
		throw new IllegalArgumentException("Bad input");
	}
}
