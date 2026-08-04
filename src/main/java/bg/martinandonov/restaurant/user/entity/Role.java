package bg.martinandonov.restaurant.user.entity;

import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
		name = "app_roles",
		uniqueConstraints = @UniqueConstraint(name = "uk_app_roles_name", columnNames = "name"))
public class Role {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Enumerated(EnumType.STRING)
	@Column(name = "name", nullable = false, length = 32)
	private RoleName name;

	protected Role() {
	}

	public Role(RoleName name) {
		this.name = Objects.requireNonNull(name, "name must not be null");
	}

	public Long getId() {
		return id;
	}

	public RoleName getName() {
		return name;
	}

	public void setName(RoleName name) {
		this.name = Objects.requireNonNull(name, "name must not be null");
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof Role role)) {
			return false;
		}
		return name == role.name;
	}

	@Override
	public int hashCode() {
		return Objects.hash(name);
	}

	@Override
	public String toString() {
		return "Role{id=" + id + ", name=" + name + "}";
	}
}