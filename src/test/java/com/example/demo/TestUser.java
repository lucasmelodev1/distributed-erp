package com.example.demo;

import org.springframework.http.ResponseCookie;

import com.example.demo.user.domain.User;

public record TestUser(User user, ResponseCookie cookie) {

}
