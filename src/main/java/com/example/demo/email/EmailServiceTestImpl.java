package com.example.demo.email;

import java.util.List;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.stereotype.Service;

import io.micrometer.observation.annotation.Observed;
import jakarta.mail.MessagingException;

@Service
@Profile("test")
public class EmailServiceTestImpl implements EmailService {
	private static final Logger log = LoggerFactory.getLogger(EmailServiceTestImpl.class);

	private final SentEmails sentEmails;

	public EmailServiceTestImpl(SentEmails sentEmails) {
		this.sentEmails = sentEmails;
	}

	/** Synchronous (unlike the real service) so tests can read the email right after the call. */
	@Override
	@Observed(name = "email.send-simple-message")
	public void sendSimpleMessage(
			String from,
			String to,
			@Nullable String subject,
			@Nullable String text) {
		SimpleMailMessage message = new SimpleMailMessage();
		message.setFrom(from);
		message.setTo(to);
		message.setSubject(subject);
		message.setText(text);
		log.info("Sending email from={} to={} subject={}", from, to, subject);
		sentEmails.record(new SentEmails.SentEmail(from, to, subject, text));
	}

	@Override
	@Observed(name = "email.send-message-with-attachment")
	public void sendMessageWithAttachment(
			String from,
			String to,
			@Nullable String subject,
			@Nullable String text,
			List<String> pathToAttachments) throws MessagingException {
		log.info("Sending email with attachment from={} to={} subject={} text={}", from, to, subject, text);
		for (String attachment : pathToAttachments) {
			log.info("Attachment: {}", attachment);
		}
	}
}