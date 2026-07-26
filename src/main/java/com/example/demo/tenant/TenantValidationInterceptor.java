package com.example.demo.tenant;

import java.io.IOException;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import com.example.demo.tenant.authorization.PermissionCheckRepository;
import com.example.demo.user.User;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class TenantValidationInterceptor implements HandlerInterceptor {
	private static final Logger log = LoggerFactory.getLogger(TenantValidationInterceptor.class);
	private static final String TENANT_ID_HEADER = "X-Tenant-Id";
	private static final String AUTH_PATH_PREFIX = "/api/v1/auth/";

	private final TenantUserRepository tenantUserRepository;
	private final PermissionCheckRepository permissionCheckRepository;

	public TenantValidationInterceptor(TenantUserRepository tenantUserRepository,
			PermissionCheckRepository permissionCheckRepository) {
		this.tenantUserRepository = tenantUserRepository;
		this.permissionCheckRepository = permissionCheckRepository;
	}

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
			throws IOException {
		if (!(handler instanceof HandlerMethod handlerMethod)) {
			return true;
		}

		String requestUri = request.getRequestURI();
		if (requestUri.startsWith(AUTH_PATH_PREFIX)) {
			return true;
		}

		TenantAware tenantAware = handlerMethod.getMethodAnnotation(TenantAware.class);
		if (tenantAware == null) {
			return true;
		}

		String tenantIdHeader = request.getHeader(TENANT_ID_HEADER);
		if (tenantIdHeader == null || tenantIdHeader.isBlank()) {
			sendUnauthorized(response, "Missing required header: " + TENANT_ID_HEADER);
			return false;
		}

		UUID tenantId;
		try {
			tenantId = UUID.fromString(tenantIdHeader);
		} catch (IllegalArgumentException e) {
			sendBadRequest(response, "Invalid UUID format for header: " + TENANT_ID_HEADER);
			return false;
		}

		var auth = SecurityContextHolder.getContext().getAuthentication();
		if (auth == null || !(auth.getPrincipal() instanceof User user)) {
			sendUnauthorized(response, "Authentication required");
			return false;
		}

		TenantUserId tenantUserId = new TenantUserId(tenantId, user.getId());
		if (!tenantUserRepository.existsById(tenantUserId)) {
			log.warn("User={} is not a member of tenant={}", user.getId(), tenantId);
			sendUnauthorized(response, "You are not a member of this tenant");
			return false;
		}

		TenantContext.setTenantId(tenantId);

		String[] requiredPermissions = tenantAware.withPermissions();
		if (requiredPermissions.length > 0) {
			UUID userId = user.getId();
			for (String permission : requiredPermissions) {
				if (!permissionCheckRepository.hasPermission(userId, tenantId, permission)) {
					log.warn("User={} lacks permission={} in tenant={}", userId, permission, tenantId);
					sendForbidden(response, "Missing required permission: " + permission);
					return false;
				}
			}
		}

		return true;
	}

	@Override
	public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler,
			Exception ex) {
		TenantContext.clear();
	}

	private void sendUnauthorized(HttpServletResponse response, String message) throws IOException {
		response.setStatus(HttpStatus.UNAUTHORIZED.value());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.getWriter().write("{\"error\":\"" + message + "\"}");
	}

	private void sendForbidden(HttpServletResponse response, String message) throws IOException {
		response.setStatus(HttpStatus.FORBIDDEN.value());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.getWriter().write("{\"error\":\"" + message + "\"}");
	}

	private void sendBadRequest(HttpServletResponse response, String message) throws IOException {
		response.setStatus(HttpStatus.BAD_REQUEST.value());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.getWriter().write("{\"error\":\"" + message + "\"}");
	}
}
