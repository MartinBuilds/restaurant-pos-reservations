package bg.martinandonov.restaurant.menu.entity;

import java.math.BigDecimal;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
		name = "menu_items",
		uniqueConstraints = @UniqueConstraint(
				name = "uk_menu_items_category_name",
				columnNames = { "category_id", "name" }))
public class MenuItem {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "name", nullable = false, length = 150)
	private String name;

	@Column(name = "description", length = 1000)
	private String description;

	@Column(name = "price", nullable = false, precision = 10, scale = 2)
	private BigDecimal price;

	@Column(name = "active", nullable = false)
	private boolean active = true;

	@Column(name = "available", nullable = false)
	private boolean available = true;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "category_id", nullable = false)
	private MenuCategory category;

	protected MenuItem() {
	}

	public MenuItem(
			String name,
			String description,
			BigDecimal price,
			boolean active,
			boolean available,
			MenuCategory category) {
		this.name = Objects.requireNonNull(name, "name must not be null");
		this.description = description;
		this.price = Objects.requireNonNull(price, "price must not be null");
		this.active = active;
		this.available = available;
		this.category = Objects.requireNonNull(category, "category must not be null");
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

	public BigDecimal getPrice() {
		return price;
	}

	public void setPrice(BigDecimal price) {
		this.price = Objects.requireNonNull(price, "price must not be null");
	}

	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}

	public boolean isAvailable() {
		return available;
	}

	public void setAvailable(boolean available) {
		this.available = available;
	}

	public MenuCategory getCategory() {
		return category;
	}

	public void setCategory(MenuCategory category) {
		this.category = Objects.requireNonNull(category, "category must not be null");
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof MenuItem item)) {
			return false;
		}
		return id != null && Objects.equals(id, item.id);
	}

	@Override
	public int hashCode() {
		return getClass().hashCode();
	}

	@Override
	public String toString() {
		return "MenuItem{id=" + id + ", name='" + name + "', price=" + price
				+ ", active=" + active + ", available=" + available + "}";
	}
}