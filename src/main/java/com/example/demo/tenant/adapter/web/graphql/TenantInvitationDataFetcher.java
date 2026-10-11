package com.example.demo.tenant.adapter.web.graphql;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.dataloader.DataLoader;

import com.example.demo.config.graphql.Connection;
import com.example.demo.config.graphql.Cursors;
import com.example.demo.config.graphql.GraphQlInputValidator;
import com.example.demo.config.graphql.GraphQlRequestContext;
import com.example.demo.config.graphql.PublicOperation;
import com.example.demo.tenant.adapter.web.dto.user.AcceptTenantInvitationInput;
import com.example.demo.tenant.adapter.web.dto.user.InviteUserToTenantInput;
import com.example.demo.tenant.adapter.web.dto.user.RegisterUserFromInvitationInput;
import com.example.demo.tenant.adapter.web.dto.user.ResendInvitationInput;
import com.example.demo.tenant.adapter.web.dto.user.RevokeInvitationInput;
import com.example.demo.tenant.config.TenantAware;
import com.example.demo.tenant.config.TenantContext;
import com.example.demo.tenant.domain.authorization.TenantRole;
import com.example.demo.tenant.domain.invitation.TenantInvitation;
import com.example.demo.tenant.port.inbound.TenantInvitationPort;
import com.example.demo.tenant.port.inbound.TenantInvitationPort.InvitationCheck;
import com.example.demo.user.adapter.web.dto.auth.LoginInput;
import com.example.demo.user.adapter.web.dto.auth.LoginPayload;
import com.example.demo.user.port.inbound.AuthPort;
import com.netflix.graphql.dgs.DgsComponent;
import com.netflix.graphql.dgs.DgsData;
import com.netflix.graphql.dgs.DgsDataFetchingEnvironment;
import com.netflix.graphql.dgs.DgsMutation;
import com.netflix.graphql.dgs.DgsQuery;
import com.netflix.graphql.dgs.InputArgument;

@DgsComponent
public class TenantInvitationDataFetcher {
	private final TenantInvitationPort invitationService;
	private final AuthPort authService;
	private final GraphQlInputValidator validator;

	public TenantInvitationDataFetcher(TenantInvitationPort invitationService, AuthPort authService,
			GraphQlInputValidator validator) {
		this.invitationService = invitationService;
		this.authService = authService;
		this.validator = validator;
	}

	@TenantAware(withPermissions = "tenants:invite")
	@DgsQuery
	public Connection<TenantInvitation> tenantInvitations(@InputArgument Integer first,
			@InputArgument String after, @InputArgument TenantInvitation.Status status,
			@InputArgument String query) {
		var window = invitationService.scrollInvitations(status, query, Cursors.pageSize(first), Cursors.decode(after),
				TenantContext.getTenantId());
		return Cursors.toConnection(window, TenantInvitation::getId, after);
	}

	@PublicOperation
	@DgsQuery
	public InvitationCheck checkInvitedUser(@InputArgument String code, DgsDataFetchingEnvironment dfe) {
		return invitationService.checkInvitedUser(code, GraphQlRequestContext.currentUserOrNull(dfe));
	}

	@DgsData(parentType = "TenantInvitation", field = "roles")
	public CompletableFuture<List<TenantRole>> roles(DgsDataFetchingEnvironment dfe) {
		TenantInvitation invitation = dfe.getSource();
		DataLoader<TenantInvitationRolesLoader.Key, List<TenantRole>> loader = dfe
				.getDataLoader(TenantInvitationRolesLoader.NAME);
		return loader.load(new TenantInvitationRolesLoader.Key(invitation.getId(), invitation.getTenantId()));
	}

	@TenantAware(withPermissions = "tenants:invite")
	@DgsMutation
	public Map<String, Object> inviteUserToTenant(@InputArgument InviteUserToTenantInput input,
			DgsDataFetchingEnvironment dfe) {
		validator.validate(input);
		return Map.of("invitation", invitationService.inviteUserToTenant(input.email(),
				input.tenantRoleIds(), GraphQlRequestContext.currentUser(dfe).getId(),
				TenantContext.getTenantId()));
	}

	@TenantAware(withPermissions = "tenants:invite")
	@DgsMutation
	public Map<String, Object> revokeInvitation(@InputArgument RevokeInvitationInput input) {
		validator.validate(input);
		return Map.of("invitation",
				invitationService.revokeInvitation(input.invitationId(), TenantContext.getTenantId()));
	}

	@TenantAware(withPermissions = "tenants:invite")
	@DgsMutation
	public Map<String, Object> resendInvitation(@InputArgument ResendInvitationInput input) {
		validator.validate(input);
		return Map.of("invitation",
				invitationService.resendInvitation(input.invitationId(), TenantContext.getTenantId()));
	}

	@DgsMutation
	public Map<String, Object> acceptTenantInvitation(@InputArgument AcceptTenantInvitationInput input,
			DgsDataFetchingEnvironment dfe) {
		validator.validate(input);
		return Map.of("tenant",
				invitationService.acceptInvitation(input.code(), GraphQlRequestContext.currentUser(dfe)));
	}

	@PublicOperation
	@DgsMutation
	public LoginPayload registerUserFromInvitation(@InputArgument RegisterUserFromInvitationInput input,
			DgsDataFetchingEnvironment dfe) {
		validator.validate(input);
		var registered = invitationService.registerFromInvitation(input.code(), input.name(), input.password());
		return authService.login(new LoginInput(registered.email(), input.password()),
				GraphQlRequestContext.httpRequest(dfe), GraphQlRequestContext.httpResponse(dfe));
	}
}
