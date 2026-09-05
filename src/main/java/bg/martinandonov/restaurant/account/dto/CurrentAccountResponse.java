package bg.martinandonov.restaurant.account.dto;

import java.util.Set;

public class CurrentAccountResponse {

	private final Long id;
	private final String name;
	private final String email;
	private final Set<String> roles;

	public CurrentAccountResponse(Long id, String name, String email, Set<String> roles) {
		this.id = id;
		this.name = name;
		this.email = email;
		this.roles = roles;
	}

	public Long getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public String getEmail() {
		return email;
	}

	public Set<String> getRoles() {
		return roles;
	}
}
