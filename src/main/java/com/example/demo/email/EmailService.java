package com.example.demo.email;

import java.util.List;

import org.jspecify.annotations.Nullable;

import jakarta.mail.MessagingException;

public interface EmailService {
	public void sendSimpleMessage(String from, String to, @Nullable String subject, @Nullable String text);

	public void sendMessageWithAttachment(String from, String to, @Nullable String subject, @Nullable String text,
			List<String> pathToAttachments) throws MessagingException;
}
