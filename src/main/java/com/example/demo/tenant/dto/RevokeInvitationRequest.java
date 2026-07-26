package com.example.demo.tenant.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;

public record RevokeInvitationRequest(@org.hibernate.validator.constraints.UUID @NotBlank UUID invitationId) {

}
