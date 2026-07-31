package com.example.demo.user.dto;

import java.util.UUID;

public record RequestResetPasswordResponse(UUID authTokenId) {
}