package bg.martinandonov.restaurant.inventory.dto;

import java.math.BigDecimal;

public class RecipeComponentResponse {

	private final Long ingredientId;
	private final String ingredientName;
	private final String unit;
	private final BigDecimal quantityRequired;

	public RecipeComponentResponse(
			Long ingredientId,
			String ingredientName,
			String unit,
			BigDecimal quantityRequired) {
		this.ingredientId = ingredientId;
		this.ingredientName = ingredientName;
		this.unit = unit;
		this.quantityRequired = quantityRequired;
	}

	public Long getIngredientId() {
		return ingredientId;
	}

	public String getIngredientName() {
		return ingredientName;
	}

	public String getUnit() {
		return unit;
	}

	public BigDecimal getQuantityRequired() {
		return quantityRequired;
	}
}
