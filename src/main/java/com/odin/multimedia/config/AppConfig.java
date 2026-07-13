package com.odin.multimedia.config;

import java.time.Clock;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

@Configuration
@EnableConfigurationProperties({StatusImageProperties.class, StatusDurationProperties.class,
		StatusMediaFilesystemProperties.class, ProfileImageKafkaProperties.class})
public class AppConfig {

	@Bean
	public Clock statusClock() {
		return Clock.systemUTC();
	}

	@Bean
	public RestTemplate restTemplate() {
		return new RestTemplate();
	}

	@Bean("statusProfileRestTemplate")
	public RestTemplate statusProfileRestTemplate() {
		SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
		factory.setConnectTimeout(3000);
		factory.setReadTimeout(3000);
		return new RestTemplate(factory);
	}
}
