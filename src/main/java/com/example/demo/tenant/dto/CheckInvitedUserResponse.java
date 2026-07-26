package com.example.demo.tenant.dto;

public record CheckInvitedUserResponse(
		CheckInvitedUserNextAction nextAction) {
	public enum CheckInvitedUserNextAction {
		CREATE_ACCOUNT,
		LOGIN,
		ACCEPT,
	}
}
