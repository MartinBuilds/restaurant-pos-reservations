package bg.martinandonov.restaurant.common;

import java.util.Map;
import java.util.Objects;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import bg.martinandonov.restaurant.user.dto.RegisterClientRequest;
import bg.martinandonov.restaurant.user.dto.UserResponse;
import bg.martinandonov.restaurant.user.service.UserService;

@RestController
@RequestMapping("/api/public")
public class PublicController {

	private final UserService userService;

	public PublicController(UserService userService) {
		this.userService = userService;
	}

	@GetMapping("/health")
	public ResponseEntity<Map<String, String>> health() {
		return ResponseEntity.ok(Map.of("status", "UP"));
	}

	@PostMapping("/register")
	public ResponseEntity<UserResponse> register(@RequestBody RegisterClientRequest request) {
		Objects.requireNonNull(request, "request must not be null");
		UserResponse created = userService.registerClient(request);
		return ResponseEntity.status(HttpStatus.CREATED).body(created);
	}
}
