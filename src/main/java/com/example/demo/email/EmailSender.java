package com.example.demo.email;

/** The last hop of the email flow, called by the Kafka consumer. */
public interface EmailSender {
	/**
	 * @throws PermanentEmailFailure when retrying cannot help (for example an invalid address)
	 * @throws RuntimeException any other failure is treated as transient and retried
	 */
	void send(EmailMessage message);
}
