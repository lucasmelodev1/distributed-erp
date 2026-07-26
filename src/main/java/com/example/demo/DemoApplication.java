package com.example.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableAsync;

import com.example.demo.telemetry.FilterConfiguration;
import com.example.demo.telemetry.OpenTelemetryConfiguration;

@EnableAsync
@SpringBootApplication
@Import({ OpenTelemetryConfiguration.class, FilterConfiguration.class })
public class DemoApplication {

	public static void main(String[] args) {
		SpringApplication.run(DemoApplication.class, args);
	}

}
