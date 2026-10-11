package com.example.demo.tenant.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.example.demo.tenant.domain.authorization.TenantRole;

class TenantRoleTest {
	static Stream<Arguments> changes() {
		return Stream.of(
				Arguments.of("rename", (Consumer<TenantRole>) role -> role.setName("Renamed")),
				Arguments.of("inactivate", (Consumer<TenantRole>) TenantRole::inactivate),
				Arguments.of("reactivate", (Consumer<TenantRole>) TenantRole::reactivate));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("changes")
	void theBuiltInRole_cannotBeChanged(String change, Consumer<TenantRole> apply) {
		var admin = TenantRole.builtIn(UUID.randomUUID(), "Admin", Set.of());

		assertThatThrownBy(() -> apply.accept(admin)).isInstanceOf(IllegalArgumentException.class);
		assertThat(admin.getName()).isEqualTo("Admin");
		assertThat(admin.isActive()).isTrue();
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("changes")
	void aCustomRole_canBeChanged(String change, Consumer<TenantRole> apply) {
		apply.accept(new TenantRole(UUID.randomUUID(), "Custom", Set.of()));
	}

	@Test
	void inactivatingTwice_keepsTheFirstTimestamp_andReactivatingRestoresIt() {
		var role = new TenantRole(UUID.randomUUID(), "Custom", Set.of());

		role.inactivate();
		var first = role.getInactivatedAt();
		role.inactivate();

		assertThat(role.isActive()).isFalse();
		assertThat(role.getInactivatedAt()).isEqualTo(first);

		role.reactivate();
		assertThat(role.isActive()).isTrue();
		assertThat(role.getInactivatedAt()).isNull();
	}
}
