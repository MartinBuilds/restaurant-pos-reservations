package bg.martinandonov.restaurant.user.dto;

import java.util.Set;

public class UserResponse {

	private final Long id;
	private final String email;
	private final String fullName;
	private final boolean enabled;
	private final Set<String> roles;

	public UserResponse(Long id, String email, String fullName, boolean enabled, Set<String> roles) {
		this.id = id;
		this.email = email;
		this.fullName = fullName;
		this.enabled = enabled;
		this.roles = roles;
	}

	public Long getId() {
		return id;
	}

	public String getEmail() {
		return email;
	}

	public String getFullName() {
		return fullName;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public Set<String> getRoles() {
		return roles;
	}
}