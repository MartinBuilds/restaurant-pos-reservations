package bg.martinandonov.restaurant.inventory.dto;

import java.math.BigDecimal;

public class AdjustIngredientStockRequest {

	private BigDecimal quantityChange;
	private String note;

	public BigDecimal getQuantityChange() {
		return quantityChange;
	}

	public void setQuantityChange(BigDecimal quantityChange) {
		this.quantityChange = quantityChange;
	}

	public String getNote() {
		return note;
	}

	public void setNote(String note) {
		this.note = note;
	}
}
