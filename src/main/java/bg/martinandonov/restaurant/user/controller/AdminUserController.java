package bg.martinandonov.restaurant.user.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import bg.martinandonov.restaurant.user.dto.CreateUserRequest;
import bg.martinandonov.restaurant.user.dto.UpdateUserRolesRequest;
import bg.martinandonov.restaurant.user.dto.UpdateUserStatusRequest;
import bg.martinandonov.restaurant.user.dto.UserResponse;
import bg.martinandonov.restaurant.user.service.UserService;

@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {

	private final UserService userService;

	public AdminUserController(UserService userService) {
		this.userService = userService;
	}

	@PostMapping
	public ResponseEntity<UserResponse> createUser(@RequestBody CreateUserRequest request) {
		UserResponse created = userService.createUser(request);
		return ResponseEntity.status(HttpStatus.CREATED).body(created);
	}

	@GetMapping
	public ResponseEntity<List<UserResponse>> getAllUsers() {
		return ResponseEntity.ok(userService.getAllUsers());
	}

	@GetMapping("/{id}")
	public ResponseEntity<UserResponse> getUserById(@PathVariable Long id) {
		return ResponseEntity.ok(userService.getUserById(id));
	}

	@PutMapping("/{id}/roles")
	public ResponseEntity<UserResponse> updateUserRoles(
			@PathVariable Long id,
			@RequestBody UpdateUserRolesRequest request) {
		return ResponseEntity.ok(userService.updateUserRoles(id, request));
	}

	@PatchMapping("/{id}/status")
	public ResponseEntity<UserResponse> updateUserStatus(
			@PathVariable Long id,
			@RequestBody UpdateUserStatusRequest request) {
		return ResponseEntity.ok(userService.updateUserStatus(id, request));
	}
}
