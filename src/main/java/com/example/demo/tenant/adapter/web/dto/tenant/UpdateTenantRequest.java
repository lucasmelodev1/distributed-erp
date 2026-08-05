package com.example.demo.tenant.adapter.web.dto.tenant;

import com.example.demo.tenant.domain.Tenant;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateTenantRequest(
		@NotBlank @Size(max = Tenant.MAX_NAME_SIZE) String name) {

}
