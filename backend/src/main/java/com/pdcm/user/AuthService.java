package com.pdcm.user;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pdcm.common.ConflictException;
import com.pdcm.common.UnauthorizedException;
import com.pdcm.security.JwtService;

@Service
public class AuthService {

	private final UserAccountRepository userAccountRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtService jwtService;

	public AuthService(
			UserAccountRepository userAccountRepository,
			PasswordEncoder passwordEncoder,
			JwtService jwtService) {
		this.userAccountRepository = userAccountRepository;
		this.passwordEncoder = passwordEncoder;
		this.jwtService = jwtService;
	}

	@Transactional
	public AuthResponse register(RegisterRequest request) {
		String email = request.email().trim().toLowerCase();
		if (userAccountRepository.existsByEmailIgnoreCase(email)) {
			throw new ConflictException("Email already registered");
		}

		UserAccount user = new UserAccount(
				email,
				request.displayName().trim(),
				passwordEncoder.encode(request.password()),
				Role.USER);

		try {
			user = userAccountRepository.saveAndFlush(user);
		}
		catch (DataIntegrityViolationException ex) {
			throw new ConflictException("Email already registered");
		}

		return AuthResponse.bearer(jwtService.createAccessToken(user), jwtService.expiresInSeconds(), user);
	}

	@Transactional(readOnly = true)
	public AuthResponse login(LoginRequest request) {
		String email = request.email().trim().toLowerCase();
		UserAccount user = userAccountRepository.findByEmailIgnoreCase(email)
				.filter(u -> passwordEncoder.matches(request.password(), u.getPasswordHash()))
				.orElseThrow(() -> new UnauthorizedException("Invalid email or password"));

		return AuthResponse.bearer(jwtService.createAccessToken(user), jwtService.expiresInSeconds(), user);
	}

	@Transactional(readOnly = true)
	public UserResponse me(Long userId) {
		UserAccount user = userAccountRepository.findById(userId)
				.orElseThrow(() -> new UnauthorizedException("User not found"));
		return UserResponse.from(user);
	}
}
