package com.example.demo.user.adapter.inbound;
import com.example.demo.user.port.inbound.UserProfilePort;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.user.adapter.outbound.persistence.UserRepository;
import com.example.demo.user.adapter.web.dto.user.CreateUserRequest;
import com.example.demo.user.adapter.web.dto.user.CreateUserResponse;
import com.example.demo.user.adapter.web.dto.user.FindMeResponse;
import com.example.demo.user.adapter.web.dto.user.UpdateUserRequest;
import com.example.demo.user.domain.User;

@Service
public class UserService implements UserProfilePort {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public FindMeResponse findMe(User user) {
        return toProfile(user);
    }

    @Transactional(readOnly = true)
    public boolean existsByEmail(String email) {
        return userRepository.findByEmail(email.trim()).isPresent();
    }

    @Transactional(readOnly = true)
    public Map<UUID, FindMeResponse> findProfilesByIds(Collection<UUID> ids) {
        return userRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(User::getId, UserService::toProfile));
    }

    private static FindMeResponse toProfile(User user) {
        return new FindMeResponse(user.getId(), user.getName(), user.getEmail(), user.getProfileIconUrl());
    }

    @Transactional
    public CreateUserResponse createUser(CreateUserRequest dto) {
        var user = new User(dto.name(), dto.email(), passwordEncoder.encode(dto.password()));
        user = userRepository.save(user);
        return new CreateUserResponse(user.getId());
    }

    @Transactional
    public void updateUser(UpdateUserRequest dto, User user) {
        user.setName(dto.name());
        userRepository.save(user);
    }
}
