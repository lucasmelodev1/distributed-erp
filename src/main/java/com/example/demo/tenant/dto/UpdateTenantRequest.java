package com.example.demo.tenant.dto;

import com.example.demo.tenant.Tenant;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateTenantRequest(
		@NotBlank @Size(max = Tenant.MAX_NAME_SIZE) String name) {

}
