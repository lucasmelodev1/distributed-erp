package com.example.demo.telemetry;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.support.ContextPropagatingTaskDecorator;

/**
 * ContextPropagationConfiguration
 *
 * @brief Maintains the context of a request even when using @Async so that telemetry is consistent
 */
@Configuration(proxyBeanMethods = false)
public class ContextPropagationConfiguration {

		@Bean
		ContextPropagatingTaskDecorator contextPropagatingTaskDecorator() {
				return new ContextPropagatingTaskDecorator();
		}

}
