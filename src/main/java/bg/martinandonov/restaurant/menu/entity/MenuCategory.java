package bg.martinandonov.restaurant.menu.entity;

import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
		name = "menu_categories",
		uniqueConstraints = @UniqueConstraint(name = "uk_menu_categories_name", columnNames = "name"))
public class MenuCategory {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "name", nullable = false, length = 100)
	private String name;

	@Column(name = "description", length = 500)
	private String description;

	@Column(name = "active", nullable = false)
	private boolean active = true;

	protected MenuCategory() {
	}

	public MenuCategory(String name, String description, boolean active) {
		this.name = Objects.requireNonNull(name, "name must not be null");
		this.description = description;
		this.active = active;
	}

	public Long getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = Objects.requireNonNull(name, "name must not be null");
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof MenuCategory category)) {
			return false;
		}
		return id != null && Objects.equals(id, category.id);
	}

	@Override
	public int hashCode() {
		return getClass().hashCode();
	}

	@Override
	public String toString() {
		return "MenuCategory{id=" + id + ", name='" + name + "', active=" + active + "}";
	}
}