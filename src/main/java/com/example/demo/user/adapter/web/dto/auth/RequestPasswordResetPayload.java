package com.example.demo.user.adapter.web.dto.auth;

import java.util.UUID;

public record RequestPasswordResetPayload(UUID authTokenId) {
}