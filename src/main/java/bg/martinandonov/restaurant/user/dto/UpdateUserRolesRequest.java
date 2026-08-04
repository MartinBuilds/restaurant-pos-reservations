package bg.martinandonov.restaurant.user.dto;

import java.util.Set;

public class UpdateUserRolesRequest {

	private Set<String> roles;

	public Set<String> getRoles() {
		return roles;
	}

	public void setRoles(Set<String> roles) {
		this.roles = roles;
	}
}