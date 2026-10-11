package com.example.demo.email;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

import org.jspecify.annotations.Nullable;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/** Emails "sent" (delivered by the consumer) in the test profile, so tests can read the tokens and links users would receive. */
@Component
@Profile("test")
public class SentEmails {
	public record SentEmail(String from, String to, @Nullable String subject, @Nullable String text) {
	}

	private static final Duration TIMEOUT = Duration.ofSeconds(15);

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

	/**
	 * Text of the most recent email sent to this address. Emails travel through Kafka, so this
	 * waits for the delivery; fails the test if none arrives in time.
	 */
	public String lastTextTo(String to) {
		return awaitTo(to).text();
	}

	/** The most recent email sent to this address, waiting for it to be delivered. */
	public SentEmail awaitTo(String to) {
		var deadline = Instant.now().plus(TIMEOUT);
		while (true) {
			var found = lastTo(to);
			if (found.isPresent()) {
				return found.get();
			}
			if (Instant.now().isAfter(deadline)) {
				throw new IllegalStateException("No email was sent to " + to + " within " + TIMEOUT);
			}
			try {
				Thread.sleep(25);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new IllegalStateException("Interrupted while waiting for an email to " + to, e);
			}
		}
	}

	public void clear() {
		emails.clear();
	}
}
