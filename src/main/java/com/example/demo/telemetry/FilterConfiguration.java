package com.example.demo.telemetry;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.micrometer.tracing.Tracer;

/**
 * Registers the {@link HeaderLoggerFilter} and {@link AddTraceIdFilter}.
 */
@Configuration(proxyBeanMethods = false)
public class FilterConfiguration {

		@Bean
		HeaderLoggerFilter headerLoggerFilter() {
				return new HeaderLoggerFilter();
		}

		@Bean
		TraceIdFilter addTraceIdFilter(Tracer tracer) {
				return new TraceIdFilter(tracer);
		}

}
