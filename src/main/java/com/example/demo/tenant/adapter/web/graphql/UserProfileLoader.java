package com.example.demo.tenant.adapter.web.graphql;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import org.dataloader.MappedBatchLoader;

import com.example.demo.user.adapter.web.dto.user.FindMeResponse;
import com.example.demo.user.port.inbound.UserProfilePort;
import com.netflix.graphql.dgs.DgsDataLoader;

/** Loads the public profiles of many users with one query. Users that do not exist are simply absent. */
@DgsDataLoader(name = UserProfileLoader.NAME)
public class UserProfileLoader implements MappedBatchLoader<UUID, FindMeResponse> {
	public static final String NAME = "userProfiles";

	private final UserProfilePort userService;

	public UserProfileLoader(UserProfilePort userService) {
		this.userService = userService;
	}

	@Override
	public CompletionStage<Map<UUID, FindMeResponse>> load(Set<UUID> userIds) {
		return CompletableFuture.completedFuture(userService.findProfilesByIds(userIds));
	}
}
