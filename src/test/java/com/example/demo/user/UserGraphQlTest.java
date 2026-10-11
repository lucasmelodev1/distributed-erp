package com.example.demo.user;

import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.example.demo.BaseGraphQlTest;
import com.example.demo.TestUser;
import com.example.demo.user.domain.User;

class UserGraphQlTest extends BaseGraphQlTest {
	private static final String UPDATE = """
			mutation($input: UpdateMeInput!) { updateMe(input: $input) { user { id name email } } }""";
	private static final String ME = "{ me { name email } }";

	private TestUser renamer;
	private TestUser bystander;

	@BeforeAll
	void setup() {
		renamer = createUser("Old Name", "gql.user.renamer@mail.com");
		bystander = createUser("Bystander", "gql.user.bystander@mail.com");
	}

	private static Map<String, Object> input(String name) {
		return variables("input", variables("name", name));
	}

	@Test
	void updateMe_renamesTheCallerAndMeShowsItImmediately() {
		graphqlAs(UPDATE, input("New Name"), renamer)
				.expectBody()
				.jsonPath("$.errors").doesNotExist()
				.jsonPath("$.data.updateMe.user.name").isEqualTo("New Name")
				.jsonPath("$.data.updateMe.user.email").isEqualTo("gql.user.renamer@mail.com");

		// The session still holds the old snapshot, so this proves me reads the database.
		graphqlAs(ME, Map.of(), renamer).expectBody().jsonPath("$.data.me.name").isEqualTo("New Name");
		graphqlAs(ME, Map.of(), bystander).expectBody().jsonPath("$.data.me.name").isEqualTo("Bystander");
	}

	/** The constraints themselves are covered by InputValidationTest; this proves the fetcher applies them. */
	@Test
	void updateMe_withAnInvalidName_isBadRequest() {
		assertGraphQlError(graphqlAs(UPDATE, input(""), bystander), "BAD_REQUEST");
		graphqlAs(ME, Map.of(), bystander).expectBody().jsonPath("$.data.me.name").isEqualTo("Bystander");
	}

	@Test
	void updateMe_requiresAuthentication() {
		assertGraphQlError(graphql(UPDATE, input("Anonymous")), "UNAUTHORIZED");
	}
}
