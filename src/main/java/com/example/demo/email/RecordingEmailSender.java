package com.example.demo.email;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

/**
 * The test profile's last hop: instead of talking SMTP it records the email in {@link SentEmails}.
 * Everything before it (Kafka, retries, the dead-letter topic) is the real thing.
 */
@Service
@Profile("test")
public class RecordingEmailSender implements EmailSender {
	private final SentEmails sentEmails;
	private final AtomicReference<Supplier<? extends RuntimeException>> failure = new AtomicReference<>();
	private final AtomicInteger remainingFailures = new AtomicInteger();

	RecordingEmailSender(SentEmails sentEmails) {
		this.sentEmails = sentEmails;
	}

	@Override
	public void send(EmailMessage message) {
		if (remainingFailures.getAndUpdate(n -> n > 0 ? n - 1 : 0) > 0) {
			throw failure.get().get();
		}
		sentEmails.record(new SentEmails.SentEmail(message.from(), message.to(), message.subject(), message.text()));
	}

	/** Makes the next {@code times} sends fail with the given exception. */
	public void failNext(int times, Supplier<? extends RuntimeException> exception) {
		failure.set(exception);
		remainingFailures.set(times);
	}

	public void reset() {
		remainingFailures.set(0);
	}
}
