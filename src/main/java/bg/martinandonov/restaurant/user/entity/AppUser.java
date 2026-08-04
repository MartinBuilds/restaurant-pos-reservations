package bg.martinandonov.restaurant.user.entity;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
		name = "app_users",
		uniqueConstraints = @UniqueConstraint(name = "uk_app_users_email", columnNames = "email"))
public class AppUser {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "email", nullable = false, length = 255)
	private String email;

	@Column(name = "password", nullable = false, length = 100)
	private String password;

	@Column(name = "full_name", nullable = false, length = 150)
	private String fullName;

	@Column(name = "enabled", nullable = false)
	private boolean enabled = true;

	@ManyToMany(fetch = FetchType.EAGER)
	@JoinTable(
			name = "app_user_roles",
			joinColumns = @JoinColumn(name = "user_id"),
			inverseJoinColumns = @JoinColumn(name = "role_id"),
			uniqueConstraints = @UniqueConstraint(
					name = "uk_app_user_roles_user_role",
					columnNames = { "user_id", "role_id" }))
	private Set<Role> roles = new HashSet<>();

	protected AppUser() {
	}

	public AppUser(String email, String password, String fullName, boolean enabled) {
		this.email = Objects.requireNonNull(email, "email must not be null");
		this.password = Objects.requireNonNull(password, "password must not be null");
		this.fullName = Objects.requireNonNull(fullName, "fullName must not be null");
		this.enabled = enabled;
	}

	public Long getId() {
		return id;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = Objects.requireNonNull(email, "email must not be null");
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = Objects.requireNonNull(password, "password must not be null");
	}

	public String getFullName() {
		return fullName;
	}

	public void setFullName(String fullName) {
		this.fullName = Objects.requireNonNull(fullName, "fullName must not be null");
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public Set<Role> getRoles() {
		return roles;
	}

	public void setRoles(Set<Role> roles) {
		this.roles = roles == null ? new HashSet<>() : new HashSet<>(roles);
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof AppUser appUser)) {
			return false;
		}
		return id != null && Objects.equals(id, appUser.id);
	}

	@Override
	public int hashCode() {
		return getClass().hashCode();
	}

	@Override
	public String toString() {
		return "AppUser{id=" + id + ", email='" + email + "', fullName='" + fullName
				+ "', enabled=" + enabled + "}";
	}
}