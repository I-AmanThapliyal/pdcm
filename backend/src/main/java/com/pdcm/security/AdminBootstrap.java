package com.pdcm.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.pdcm.user.Role;
import com.pdcm.user.UserAccount;
import com.pdcm.user.UserAccountRepository;

@Component
@EnableConfigurationProperties(AdminBootstrap.AdminProperties.class)
public class AdminBootstrap implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);

	private final UserAccountRepository userAccountRepository;
	private final PasswordEncoder passwordEncoder;
	private final AdminProperties adminProperties;

	public AdminBootstrap(
			UserAccountRepository userAccountRepository,
			PasswordEncoder passwordEncoder,
			AdminProperties adminProperties) {
		this.userAccountRepository = userAccountRepository;
		this.passwordEncoder = passwordEncoder;
		this.adminProperties = adminProperties;
	}

	@Override
	public void run(ApplicationArguments args) {
		if (!StringUtils.hasText(adminProperties.password())) {
			log.info("PDCM_ADMIN_PASSWORD not set; skipping admin bootstrap");
			return;
		}
		String email = adminProperties.email().trim().toLowerCase();
		if (userAccountRepository.existsByEmailIgnoreCase(email)) {
			return;
		}
		userAccountRepository.save(new UserAccount(
				email,
				adminProperties.displayName(),
				passwordEncoder.encode(adminProperties.password()),
				Role.ADMIN));
		log.info("Bootstrapped ADMIN user {}", email);
	}

	@ConfigurationProperties(prefix = "pdcm.admin")
	public record AdminProperties(
			String email,
			String password,
			String displayName
	) {
	}
}
