package com.example.demo.user;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.user.dto.CreateUserRequest;
import com.example.demo.user.dto.CreateUserResponse;
import com.example.demo.user.dto.FindMeResponse;
import com.example.demo.user.dto.UpdateUserRequest;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public FindMeResponse findMe(User user) {
        return new FindMeResponse(user.getId(), user.getName(), user.getEmail(), null);
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
