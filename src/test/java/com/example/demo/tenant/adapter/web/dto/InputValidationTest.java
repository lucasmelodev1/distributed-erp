package com.example.demo.tenant.adapter.web.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import com.example.demo.tenant.adapter.web.dto.role.CreateTenantRoleInput;
import com.example.demo.tenant.adapter.web.dto.role.UpdateTenantRoleInput;
import com.example.demo.tenant.adapter.web.dto.tenant.CreateTenantInput;
import com.example.demo.tenant.adapter.web.dto.tenant.UpdateTenantInput;
import com.example.demo.tenant.adapter.web.dto.user.InviteUserToTenantInput;
import com.example.demo.user.adapter.web.dto.user.UpdateMeInput;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

/**
 * The Jakarta constraints on the input records. The data fetchers apply them through
 * {@code GraphQlInputValidator}, which {@code TenantGraphQlTest} proves is wired up; every rule is checked here
 * without starting the application.
 */
class InputValidationTest {
	private static final ValidatorFactory FACTORY = Validation.buildDefaultValidatorFactory();
	private static final Validator VALIDATOR = FACTORY.getValidator();

	/** Every input that carries a display name, built around the given name. */
	private static final Map<String, Function<String, Object>> NAMED = Map.of(
			"CreateTenantInput", CreateTenantInput::new,
			"UpdateTenantInput", UpdateTenantInput::new,
			"CreateTenantRoleInput", name -> new CreateTenantRoleInput(name, List.of()),
			"UpdateTenantRoleInput", name -> new UpdateTenantRoleInput(UUID.randomUUID(), name, List.of()),
			"UpdateMeInput", UpdateMeInput::new);

	@AfterAll
	static void closeFactory() {
		FACTORY.close();
	}

	static Stream<Arguments> namedInputs() {
		return NAMED.entrySet().stream().map(e -> Arguments.of(e.getKey(), e.getValue()));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("namedInputs")
	void aReasonableName_isAccepted(String type, Function<String, Object> input) {
		assertThat(VALIDATOR.validate(input.apply("Acme"))).isEmpty();
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("namedInputs")
	void aMissingBlankOrOversizedName_isRejected(String type, Function<String, Object> input) {
		for (var name : new String[] { null, "", "   ", "x".repeat(256) }) {
			assertThat(VALIDATOR.validate(input.apply(name)))
					.as("%s with name %s", type, name == null ? "null" : "'%.10s'".formatted(name))
					.hasSize(1);
		}
	}

	@ParameterizedTest
	@ValueSource(strings = { "", "   ", "not-an-email", "missing@tld", "two@@mail.com" })
	void invitations_rejectAnInvalidEmail(String email) {
		assertThat(VALIDATOR.validate(new InviteUserToTenantInput(email, List.of(UUID.randomUUID())))).isNotEmpty();
	}

	@ParameterizedTest
	@MethodSource("noRoles")
	void invitations_needAtLeastOneRole(List<UUID> roles) {
		assertThat(VALIDATOR.validate(new InviteUserToTenantInput("someone@mail.com", roles))).hasSize(1);
	}

	static Stream<List<UUID>> noRoles() {
		return Stream.of(List.of(), null);
	}

	@ParameterizedTest
	@ValueSource(strings = { "someone@mail.com", "first.last+tag@sub.mail.co" })
	void invitations_acceptAValidRequest(String email) {
		assertThat(VALIDATOR.validate(new InviteUserToTenantInput(email, List.of(UUID.randomUUID())))).isEmpty();
	}
}
