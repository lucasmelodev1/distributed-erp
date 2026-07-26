package com.example.demo.email;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import com.example.demo.TestcontainersConfiguration;
import com.example.demo.user.User;
import com.example.demo.user.UserRepository;
import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetup;

import jakarta.mail.internet.MimeMessage;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class EmailIntegrationTest {

	private static final GreenMail greenMail = new GreenMail(new ServerSetup(0, "localhost", "smtp"));

	static {
		greenMail.start();
	}

	@DynamicPropertySource
	static void mailProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.mail.host", () -> "localhost");
		registry.add("spring.mail.port", () -> greenMail.getSmtp().getPort());
		registry.add("spring.mail.username", () -> "");
		registry.add("spring.mail.password", () -> "");
		registry.add("spring.mail.properties.mail.smtp.auth", () -> "false");
		registry.add("spring.mail.properties.mail.smtp.starttls.enable", () -> "false");
		registry.add("AUTH_EMAIL_FROM", () -> "noreply@example.com");
	}

	@AfterAll
	static void stopGreenMail() {
		greenMail.stop();
	}

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JavaMailSender mailSender;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@BeforeEach
	void purgeGreenMail() throws com.icegreen.greenmail.store.FolderException {
		greenMail.purgeEmailFromAllMailboxes();
	}

	@Test
	void greenMail_receivesDirectEmail() throws Exception {
		SimpleMailMessage message = new SimpleMailMessage();
		message.setFrom("noreply@example.com");
		message.setTo("user@example.com");
		message.setSubject("Test Subject");
		message.setText("Test Body");
		mailSender.send(message);

		MimeMessage[] messages = greenMail.getReceivedMessages();
		assertEquals(1, messages.length);
		assertEquals("Test Subject", messages[0].getSubject());
	}

	@Test
	void requestEmailVerification_sendsEmail() throws Exception {
		mockMvc.perform(MockMvcRequestBuilders
						.post("/api/v1/auth/register-email-verification")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"email\":\"user@example.com\"}"))
				.andExpect(MockMvcResultMatchers.status().isOk());

		Thread.sleep(2000);

		MimeMessage[] messages = greenMail.getReceivedMessages();
		assertEquals(1, messages.length);

		MimeMessage email = messages[0];
		assertEquals("noreply@example.com", email.getFrom()[0].toString());
		assertEquals("user@example.com", email.getAllRecipients()[0].toString());
		assertNotNull(email.getSubject());
	}

	@Test
	void requestResetPassword_existingUser_sendsEmail() throws Exception {
		String email = "reset@example.com";
		User user = new User("Reset User", email, passwordEncoder.encode("Password@123"));
		userRepository.save(user);

		mockMvc.perform(MockMvcRequestBuilders
						.post("/api/v1/auth/request-reset-password")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"email\":\"" + email + "\"}"))
				.andExpect(MockMvcResultMatchers.status().isOk());

		Thread.sleep(2000);

		MimeMessage[] messages = greenMail.getReceivedMessages();
		assertEquals(1, messages.length);

		MimeMessage emailMessage = messages[0];
		assertEquals(email, emailMessage.getAllRecipients()[0].toString());
	}
}
