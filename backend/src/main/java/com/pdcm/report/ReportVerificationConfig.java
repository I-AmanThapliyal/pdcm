package com.pdcm.report;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(ReportVerificationProperties.class)
public class ReportVerificationConfig {
}
