package com.example.demo.tenant.adapter.web.dto.user;

public record CheckInvitedUserResponse(
		CheckInvitedUserNextAction nextAction) {
	public enum CheckInvitedUserNextAction {
		CREATE_ACCOUNT,
		LOGIN,
		ACCEPT,
	}
}
