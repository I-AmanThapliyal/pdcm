package com.pdcm.common;

import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import tools.jackson.databind.DeserializationFeature;

@Configuration
public class JacksonConfig {

	@Bean
	JsonMapperBuilderCustomizer rejectFloatAsInteger() {
		return builder -> builder.disable(DeserializationFeature.ACCEPT_FLOAT_AS_INT);
	}
}
