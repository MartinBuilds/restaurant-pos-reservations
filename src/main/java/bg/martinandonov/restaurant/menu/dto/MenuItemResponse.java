package bg.martinandonov.restaurant.menu.dto;

import java.math.BigDecimal;

public class MenuItemResponse {

	private final Long id;
	private final String name;
	private final String description;
	private final BigDecimal price;
	private final boolean active;
	private final boolean manualAvailable;
	private final boolean available;
	private final String availabilityReason;
	private final Long categoryId;
	private final String categoryName;

	public MenuItemResponse(
			Long id,
			String name,
			String description,
			BigDecimal price,
			boolean active,
			boolean manualAvailable,
			boolean available,
			String availabilityReason,
			Long categoryId,
			String categoryName) {
		this.id = id;
		this.name = name;
		this.description = description;
		this.price = price;
		this.active = active;
		this.manualAvailable = manualAvailable;
		this.available = available;
		this.availabilityReason = availabilityReason;
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

	public boolean isManualAvailable() {
		return manualAvailable;
	}

	public boolean isAvailable() {
		return available;
	}

	public String getAvailabilityReason() {
		return availabilityReason;
	}

	public Long getCategoryId() {
		return categoryId;
	}

	public String getCategoryName() {
		return categoryName;
	}
}
