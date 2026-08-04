package bg.martinandonov.restaurant.inventory.dto;

import java.math.BigDecimal;

public class UpdateIngredientRequest {

	private String name;
	private String unit;
	private BigDecimal minimumStockLevel;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getUnit() {
		return unit;
	}

	public void setUnit(String unit) {
		this.unit = unit;
	}

	public BigDecimal getMinimumStockLevel() {
		return minimumStockLevel;
	}

	public void setMinimumStockLevel(BigDecimal minimumStockLevel) {
		this.minimumStockLevel = minimumStockLevel;
	}
}
