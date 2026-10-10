package com.example.demo.user.port.inbound;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

import com.example.demo.user.adapter.web.dto.user.CreateUserRequest;
import com.example.demo.user.adapter.web.dto.user.CreateUserResponse;
import com.example.demo.user.adapter.web.dto.user.FindMeResponse;
import com.example.demo.user.adapter.web.dto.user.UpdateUserRequest;
import com.example.demo.user.domain.User;

public interface UserProfilePort {
    FindMeResponse findMe(User user);
    boolean existsByEmail(String email);
    Map<UUID, FindMeResponse> findProfilesByIds(Collection<UUID> ids);
    CreateUserResponse createUser(CreateUserRequest dto);
    void updateUser(UpdateUserRequest dto, User user);
}
