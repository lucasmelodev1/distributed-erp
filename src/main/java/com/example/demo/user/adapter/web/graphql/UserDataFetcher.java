package com.example.demo.user.adapter.web.graphql;

import java.util.Map;

import com.example.demo.config.graphql.GraphQlInputValidator;
import com.example.demo.config.graphql.GraphQlRequestContext;
import com.example.demo.user.adapter.web.dto.user.UserProfile;
import com.example.demo.user.adapter.web.dto.user.UpdateMeInput;
import com.example.demo.user.port.inbound.UserProfilePort;
import com.netflix.graphql.dgs.DgsComponent;
import com.netflix.graphql.dgs.DgsDataFetchingEnvironment;
import com.netflix.graphql.dgs.DgsMutation;
import com.netflix.graphql.dgs.DgsQuery;
import com.netflix.graphql.dgs.InputArgument;

@DgsComponent
public class UserDataFetcher {
	private final UserProfilePort userService;
	private final GraphQlInputValidator validator;

	public UserDataFetcher(UserProfilePort userService, GraphQlInputValidator validator) {
		this.userService = userService;
		this.validator = validator;
	}

	@DgsQuery
	public UserProfile me(DgsDataFetchingEnvironment dfe) {
		return userService.findMe(GraphQlRequestContext.currentUser(dfe));
	}

	@DgsMutation
	public Map<String, Object> updateMe(@InputArgument UpdateMeInput input, DgsDataFetchingEnvironment dfe) {
		validator.validate(input);
		return Map.of("user", userService.updateUser(input, GraphQlRequestContext.currentUser(dfe)));
	}
}
