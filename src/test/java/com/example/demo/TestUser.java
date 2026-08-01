package com.example.demo;

import org.springframework.http.ResponseCookie;

import com.example.demo.user.User;

public record TestUser(User user, ResponseCookie cookie) {

}
