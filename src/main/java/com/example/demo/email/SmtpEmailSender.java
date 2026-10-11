package com.example.demo.email;

import org.springframework.context.annotation.Profile;
import org.springframework.mail.MailException;
import org.springframework.mail.MailParseException;
import org.springframework.mail.MailPreparationException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import jakarta.mail.SendFailedException;
import jakarta.mail.internet.AddressException;

@Service
@Profile("!test")
class SmtpEmailSender implements EmailSender {
	private final JavaMailSender mailSender;

	SmtpEmailSender(JavaMailSender mailSender) {
		this.mailSender = mailSender;
	}

	@Override
	public void send(EmailMessage email) {
		SimpleMailMessage message = new SimpleMailMessage();
		message.setFrom(email.from());
		message.setTo(email.to());
		message.setSubject(email.subject());
		message.setText(email.text());
		try {
			mailSender.send(message);
		} catch (MailException e) {
			if (isPermanent(e)) {
				throw new PermanentEmailFailure("Email to " + email.to() + " cannot be delivered", e);
			}
			throw e;
		}
	}

	/** Bad content or a rejected address will fail the same way every time; connection and auth problems may not. */
	private static boolean isPermanent(MailException e) {
		if (e instanceof MailParseException || e instanceof MailPreparationException) {
			return true;
		}
		for (Throwable cause = e; cause != null; cause = cause.getCause()) {
			if (cause instanceof AddressException) {
				return true;
			}
			if (cause instanceof SendFailedException failed && failed.getInvalidAddresses() != null
					&& failed.getInvalidAddresses().length > 0) {
				return true;
			}
		}
		return false;
	}
}
