package com.example.demo.email;

import org.jspecify.annotations.Nullable;

/**
 * Asks for an email to be sent. The email is published to Kafka (after the surrounding
 * transaction commits, if there is one) and sent later by {@link EmailConsumer}.
 */
public interface EmailService {
	public void sendSimpleMessage(String from, String to, @Nullable String subject, @Nullable String text);
}
