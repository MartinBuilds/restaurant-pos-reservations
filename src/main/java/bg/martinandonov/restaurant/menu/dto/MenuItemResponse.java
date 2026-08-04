package bg.martinandonov.restaurant.menu.dto;

import java.math.BigDecimal;

public class MenuItemResponse {

	private final Long id;
	private final String name;
	private final String description;
	private final BigDecimal price;
	private final boolean active;
	private final boolean available;
	private final Long categoryId;
	private final String categoryName;

	public MenuItemResponse(
			Long id,
			String name,
			String description,
			BigDecimal price,
			boolean active,
			boolean available,
			Long categoryId,
			String categoryName) {
		this.id = id;
		this.name = name;
		this.description = description;
		this.price = price;
		this.active = active;
		this.available = available;
		this.categoryId = categoryId;
		this.categoryName = categoryName;
	}

	public Long getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public String getDescription() {
		return description;
	}

	public BigDecimal getPrice() {
		return price;
	}

	public boolean isActive() {
		return active;
	}

	public boolean isAvailable() {
		return available;
	}

	public Long getCategoryId() {
		return categoryId;
	}

	public String getCategoryName() {
		return categoryName;
	}
}