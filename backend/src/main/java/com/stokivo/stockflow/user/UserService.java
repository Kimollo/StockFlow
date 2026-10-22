package com.stokivo.stockflow.user;

import java.util.List;

import com.stokivo.stockflow.auth.CurrentUser;
import com.stokivo.stockflow.common.ApiException;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

	private final UserRepository users;
	private final PasswordEncoder passwordEncoder;
	private final CurrentUser currentUser;

	public UserService(UserRepository users, PasswordEncoder passwordEncoder, CurrentUser currentUser) {
		this.users = users;
		this.passwordEncoder = passwordEncoder;
		this.currentUser = currentUser;
	}

	@Transactional(readOnly = true)
	public List<UserResponse> list() {
		return users.findAll().stream()
				.sorted((left, right) -> left.getName().compareToIgnoreCase(right.getName()))
				.map(UserResponse::from)
				.toList();
	}

	@Transactional
	public UserResponse create(UserRequest request) {
		if (users.existsByUsernameIgnoreCase(request.username().trim())) {
			throw new ApiException(HttpStatus.CONFLICT, "A user with this username already exists.");
		}
		User user = new User();
		user.setName(request.name().trim());
		user.setUsername(request.username().trim());
		user.setPasswordHash(passwordEncoder.encode(request.password()));
		user.setRole(request.role());
		user.setActive(true);
		return UserResponse.from(users.save(user));
	}

	@Transactional
	public UserResponse changeStatus(Long id, boolean active) {
		User user = users.findById(id)
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found."));
		User actor = currentUser.require();
		if (actor.getId().equals(user.getId()) && !active) {
			throw new ApiException(HttpStatus.CONFLICT, "You cannot deactivate your own account.");
		}
		if (!active && user.getRole() == Role.ADMINISTRATOR && user.isActive()
				&& users.countByRoleAndActiveTrue(Role.ADMINISTRATOR) <= 1) {
			throw new ApiException(HttpStatus.CONFLICT, "The shop needs at least one active administrator.");
		}
		user.setActive(active);
		return UserResponse.from(user);
	}

	@Transactional
	public UserResponse changePassword(Long id, String password) {
		User user = users.findById(id)
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found."));
		user.setPasswordHash(passwordEncoder.encode(password));
		user.setCredentialsVersion(user.getCredentialsVersion() + 1);
		return UserResponse.from(user);
	}
}
