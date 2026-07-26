package com.example.demo.email;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Properties;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

@ExtendWith(MockitoExtension.class)
class EmailServiceImplTest {

	@Mock
	private JavaMailSender emailSender;

	@InjectMocks
	private EmailServiceImpl emailService;

	@Test
	void sendSimpleMessage_sendsMailWithCorrectFields() {
		String from = "sender@example.com";
		String to = "recipient@example.com";
		String subject = "Test Subject";
		String text = "Hello World";

		emailService.sendSimpleMessage(from, to, subject, text);

		ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
		verify(emailSender).send(captor.capture());

		SimpleMailMessage sent = captor.getValue();
		assertEquals(from, sent.getFrom());
		assertArrayEquals(new String[]{ to }, sent.getTo());
		assertEquals(subject, sent.getSubject());
		assertEquals(text, sent.getText());
	}

	@Test
	void sendSimpleMessage_withNullSubject_sendsMailWithNullSubject() {
		emailService.sendSimpleMessage("from@example.com", "to@example.com", null, "text");

		ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
		verify(emailSender).send(captor.capture());

		SimpleMailMessage sent = captor.getValue();
		assertNull(sent.getSubject());
	}

	@Test
	void sendSimpleMessage_withNullText_sendsMailWithNullText() {
		emailService.sendSimpleMessage("from@example.com", "to@example.com", "subject", null);

		ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
		verify(emailSender).send(captor.capture());

		SimpleMailMessage sent = captor.getValue();
		assertNull(sent.getText());
	}

	@Test
	void sendMessageWithAttachment_sendsMail() throws Exception {
		when(emailSender.createMimeMessage()).thenReturn(new MimeMessage(Session.getDefaultInstance(new Properties())));

		emailService.sendMessageWithAttachment(
				"sender@example.com", "recipient@example.com", "Attachment Subject", "See attached", List.of());

		verify(emailSender).send(any(MimeMessage.class));
	}
}
