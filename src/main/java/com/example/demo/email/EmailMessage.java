package com.example.demo.email;

import org.jspecify.annotations.Nullable;

/** An email on its way through Kafka. Serialised as JSON on the {@link EmailTopics#SEND} topic. */
public record EmailMessage(String from, String to, @Nullable String subject, @Nullable String text) {
}
