package com.example.demo.user.port.inbound;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

import com.example.demo.user.adapter.web.dto.user.CreateUserInput;
import com.example.demo.user.adapter.web.dto.user.UserProfile;
import com.example.demo.user.adapter.web.dto.user.UpdateMeInput;
import com.example.demo.user.domain.User;

public interface UserProfilePort {
    UserProfile findMe(User user);
    boolean existsByEmail(String email);
    Map<UUID, UserProfile> findProfilesByIds(Collection<UUID> ids);
    UserProfile createUser(CreateUserInput dto);
    UserProfile updateUser(UpdateMeInput dto, User user);
}
