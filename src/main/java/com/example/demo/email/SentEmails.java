package com.example.demo.email;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

import org.jspecify.annotations.Nullable;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/** Emails "sent" in the test profile, so tests can read the tokens and links users would receive. */
@Component
@Profile("test")
public class SentEmails {
	public record SentEmail(String from, String to, @Nullable String subject, @Nullable String text) {
	}

	private final List<SentEmail> emails = new CopyOnWriteArrayList<>();

	void record(SentEmail email) {
		emails.add(email);
	}

	public List<SentEmail> all() {
		return List.copyOf(emails);
	}

	/** The most recent email sent to this address, if any. */
	public Optional<SentEmail> lastTo(String to) {
		return emails.reversed().stream().filter(email -> email.to().equalsIgnoreCase(to)).findFirst();
	}

	/** Text of the most recent email sent to this address; fails the test if none was sent. */
	public String lastTextTo(String to) {
		return lastTo(to)
				.map(SentEmail::text)
				.orElseThrow(() -> new IllegalStateException("No email was sent to " + to));
	}

	public void clear() {
		emails.clear();
	}
}
