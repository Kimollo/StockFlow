package com.stokivo.stockflow.user;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AdminSeeder implements ApplicationRunner {

	private final UserRepository users;
	private final PasswordEncoder passwordEncoder;
	private final String name;
	private final String username;
	private final String password;

	public AdminSeeder(
			UserRepository users,
			PasswordEncoder passwordEncoder,
			@Value("${stockflow.admin.name}") String name,
			@Value("${stockflow.admin.username}") String username,
			@Value("${stockflow.admin.password}") String password) {
		this.users = users;
		this.passwordEncoder = passwordEncoder;
		this.name = name;
		this.username = username;
		this.password = password;
	}

	@Override
	public void run(ApplicationArguments args) {
		if (users.count() > 0) {
			return;
		}
		User admin = new User();
		admin.setName(name);
		admin.setUsername(username.trim());
		admin.setPasswordHash(passwordEncoder.encode(password));
		admin.setRole(Role.ADMINISTRATOR);
		admin.setActive(true);
		users.save(admin);
	}
}
