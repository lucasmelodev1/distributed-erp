package com.example.demo.email;

import java.io.File;
import java.util.List;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.FileSystemResource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import io.micrometer.observation.annotation.Observed;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
@Profile("!test")
public class EmailServiceImpl implements EmailService {
	private static final Logger log = LoggerFactory.getLogger(EmailServiceImpl.class);

	private final JavaMailSender emailSender;

	public EmailServiceImpl(JavaMailSender emailSender) {
		this.emailSender = emailSender;
	}

	@Async
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
		emailSender.send(message);
	}

	@Async
	@Override
	@Observed(name = "email.send-message-with-attachment")
	public void sendMessageWithAttachment(
			String from,
			String to,
			@Nullable String subject,
			@Nullable String text,
			List<String> pathToAttachments) throws MessagingException {
		MimeMessage message = emailSender.createMimeMessage();

		MimeMessageHelper helper = new MimeMessageHelper(message, true);

		helper.setFrom(from);
		helper.setTo(to);
		helper.setSubject(subject);
		helper.setText(text);

		for (String attachment : pathToAttachments) {
			FileSystemResource file = new FileSystemResource(new File(attachment));
			try {
				helper.addAttachment("Invoice", file);
			} catch (MessagingException e) {
				log.info("Error adding attachment to email from={} to={}: {}", from, to, e.getMessage(), e);
				throw e;
			}
		}

		emailSender.send(message);
	}
}