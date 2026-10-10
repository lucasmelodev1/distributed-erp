package com.example.demo.tenant.adapter.web.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AcceptTenantInvitationInput(@NotBlank @Size(max = 64) String code) {
}
