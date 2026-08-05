package com.example.demo.tenant.adapter.web.dto.user;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;

public record RevokeInvitationRequest(@org.hibernate.validator.constraints.UUID @NotBlank UUID invitationId) {

}
