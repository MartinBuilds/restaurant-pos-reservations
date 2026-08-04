package bg.martinandonov.restaurant.inventory.dto;

import java.math.BigDecimal;

public class RecipeComponentRequest {

	private Long ingredientId;
	private BigDecimal quantityRequired;

	public Long getIngredientId() {
		return ingredientId;
	}

	public void setIngredientId(Long ingredientId) {
		this.ingredientId = ingredientId;
	}

	public BigDecimal getQuantityRequired() {
		return quantityRequired;
	}

	public void setQuantityRequired(BigDecimal quantityRequired) {
		this.quantityRequired = quantityRequired;
	}
}
