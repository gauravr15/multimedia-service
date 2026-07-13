package com.odin.multimedia;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.odin.multimedia.filters.InvalidCharacterFilter;

@SpringBootApplication
@EnableScheduling
public class VideoApplication {

	public static void main(String[] args) {
		SpringApplication.run(VideoApplication.class, args);
	}

	@Bean
	public FilterRegistrationBean<InvalidCharacterFilter> loggingFilter() {
	    FilterRegistrationBean<InvalidCharacterFilter> registrationBean = new FilterRegistrationBean<>();
	    registrationBean.setFilter(new InvalidCharacterFilter());
	    registrationBean.addUrlPatterns("/*"); // Or specify specific patterns if needed
	    return registrationBean;
	}
}
