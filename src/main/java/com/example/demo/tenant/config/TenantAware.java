package com.example.demo.tenant.config;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Target(METHOD)
@Retention(RUNTIME)
public @interface TenantAware {
	String[] withPermissions() default {};

	/** Only operations that bring an inactive tenant back (reactivation) should set this. */
	boolean allowInactiveTenant() default false;
}
