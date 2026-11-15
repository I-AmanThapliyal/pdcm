package com.pdcm.externaltraffic;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
@EnableConfigurationProperties(ExternalTrafficProperties.class)
public class ExternalTrafficConfig {

	@Bean
	@ConditionalOnMissingBean(TrafficProvider.class)
	TrafficProvider trafficProvider(ExternalTrafficProperties properties) {
		return new TomTomTrafficClient(properties);
	}
}
