package com.stokivo.stockflow.user;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {

	private final UserService userService;

	public UserController(UserService userService) {
		this.userService = userService;
	}

	@GetMapping
	public List<UserResponse> list() {
		return userService.list();
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public UserResponse create(@Valid @RequestBody UserRequest request) {
		return userService.create(request);
	}

	@PatchMapping("/{id}/status")
	public UserResponse changeStatus(@PathVariable Long id, @Valid @RequestBody UserStatusRequest request) {
		return userService.changeStatus(id, request.active());
	}

	@PatchMapping("/{id}/password")
	public UserResponse changePassword(@PathVariable Long id, @Valid @RequestBody PasswordRequest request) {
		return userService.changePassword(id, request.password());
	}
}
