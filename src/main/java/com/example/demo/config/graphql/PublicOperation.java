package com.example.demo.config.graphql;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Marks a top-level Query/Mutation data fetcher as callable without
 * authentication. Every top-level operation without this annotation requires an
 * authenticated user (default-deny). Cannot be combined with
 * {@code @TenantAware}.
 */
@Target(METHOD)
@Retention(RUNTIME)
public @interface PublicOperation {
}
