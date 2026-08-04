package bg.martinandonov.restaurant.inventory.dto;

import java.math.BigDecimal;

public class IngredientResponse {

	private final Long id;
	private final String name;
	private final String unit;
	private final BigDecimal stockQuantity;
	private final BigDecimal minimumStockLevel;
	private final boolean active;
	private final boolean lowStock;

	public IngredientResponse(
			Long id,
			String name,
			String unit,
			BigDecimal stockQuantity,
			BigDecimal minimumStockLevel,
			boolean active,
			boolean lowStock) {
		this.id = id;
		this.name = name;
		this.unit = unit;
		this.stockQuantity = stockQuantity;
		this.minimumStockLevel = minimumStockLevel;
		this.active = active;
		this.lowStock = lowStock;
	}

	public Long getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public String getUnit() {
		return unit;
	}

	public BigDecimal getStockQuantity() {
		return stockQuantity;
	}

	public BigDecimal getMinimumStockLevel() {
		return minimumStockLevel;
	}

	public boolean isActive() {
		return active;
	}

	public boolean isLowStock() {
		return lowStock;
	}
}
