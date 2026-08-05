package com.example.demo.tenant.adapter.web;

import java.util.UUID;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.tenant.adapter.web.dto.role.CreateTenantRoleRequest;
import com.example.demo.tenant.adapter.web.dto.role.CreateTenantRoleResponse;
import com.example.demo.tenant.adapter.web.dto.role.UpdateTenantRoleRequest;
import com.example.demo.tenant.port.inbound.TenantRoleManagementPort;
import com.example.demo.tenant.config.TenantAware;
import com.example.demo.tenant.config.TenantContext;
import com.example.demo.tenant.domain.authorization.TenantRole;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/tenant-role")
@Tag(name = "Tenant Role")
public class TenantRoleController {
    private final TenantRoleManagementPort tenantRoleService;

    public TenantRoleController(TenantRoleManagementPort tenantRoleService) {
        this.tenantRoleService = tenantRoleService;
    }

    @TenantAware(withPermissions = "tenant_roles:read")
    @GetMapping
    @Operation(summary = "Find all tenant roles")
    public Page<TenantRole> findAllTenantRoles(
            @RequestParam(required = false) String query,
            @ParameterObject @PageableDefault Pageable pageable) {
        var tenantId = TenantContext.getTenantId();
        return tenantRoleService.findAllTenantRoles(query, pageable, tenantId);
    }

    @TenantAware(withPermissions = "tenant_roles:read")
    @GetMapping("/{id}")
    @Operation(summary = "Find a tenant role by Id")
    public TenantRole findTenantRoleById(@RequestParam UUID id) {
        var tenantId = TenantContext.getTenantId();
        return tenantRoleService.findTenantRoleById(id, tenantId);
    }

    @TenantAware(withPermissions = "tenant_roles:create")
    @PostMapping
    @Operation(summary = "Create a new tenant role")
    public CreateTenantRoleResponse createTenantRole(@Valid @RequestBody CreateTenantRoleRequest tenantRole) {
        var tenantId = TenantContext.getTenantId();
        return tenantRoleService.createTenantRole(tenantRole, tenantId);
    }

    @TenantAware(withPermissions = "tenant_roles:update")
    @PostMapping("/{id}")
    @Operation(summary = "Update a tenant role")
    public void updateTenantRole(@RequestParam UUID id, @Valid @RequestBody UpdateTenantRoleRequest tenantRole) {
        var tenantId = TenantContext.getTenantId();
        tenantRoleService.updateTenantRole(id, tenantRole, tenantId);
    }

    @TenantAware(withPermissions = "tenant_roles:delete")
    @PostMapping("/{id}/delete")
    @Operation(summary = "Delete a tenant role")
    public void deleteTenantRole(@RequestParam UUID id) {
        var tenantId = TenantContext.getTenantId();
        tenantRoleService.deleteTenantRole(id, tenantId);
    }
}
