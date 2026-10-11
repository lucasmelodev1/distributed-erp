package com.example.demo.email;

/** A failure that retrying cannot fix. The message goes straight to the dead-letter topic. */
public class PermanentEmailFailure extends RuntimeException {
	public PermanentEmailFailure(String message, Throwable cause) {
		super(message, cause);
	}
}
