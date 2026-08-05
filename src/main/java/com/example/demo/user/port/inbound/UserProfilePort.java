package com.example.demo.user.port.inbound;

import com.example.demo.user.adapter.web.dto.user.CreateUserRequest;
import com.example.demo.user.adapter.web.dto.user.CreateUserResponse;
import com.example.demo.user.adapter.web.dto.user.FindMeResponse;
import com.example.demo.user.adapter.web.dto.user.UpdateUserRequest;
import com.example.demo.user.domain.User;

public interface UserProfilePort {
    FindMeResponse findMe(User user);
    CreateUserResponse createUser(CreateUserRequest dto);
    void updateUser(UpdateUserRequest dto, User user);
}
