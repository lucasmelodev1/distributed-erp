package com.example.demo.tenant.adapter.web.dto.user;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record RevokeInvitationInput(@NotNull UUID invitationId) {
}
