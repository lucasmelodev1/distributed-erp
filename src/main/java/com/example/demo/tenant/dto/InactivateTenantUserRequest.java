package com.example.demo.tenant.dto;

import jakarta.validation.constraints.NotBlank;

public record InactivateTenantUserRequest(@NotBlank boolean active) {

}
