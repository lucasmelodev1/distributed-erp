package com.example.demo.tenant.adapter.web.dto.user;

import jakarta.validation.constraints.NotBlank;

public record InactivateTenantUserRequest(@NotBlank boolean active) {

}
