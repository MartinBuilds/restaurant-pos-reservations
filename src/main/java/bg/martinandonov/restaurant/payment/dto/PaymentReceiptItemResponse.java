package bg.martinandonov.restaurant.payment.dto;

import java.math.BigDecimal;

public class PaymentReceiptItemResponse {

	private final Long orderItemId;
	private final Long menuItemId;
	private final String menuItemName;
	private final BigDecimal unitPrice;
	private final Integer quantity;
	private final BigDecimal lineTotal;

	public PaymentReceiptItemResponse(
			Long orderItemId,
			Long menuItemId,
			String menuItemName,
			BigDecimal unitPrice,
			Integer quantity,
			BigDecimal lineTotal) {
		this.orderItemId = orderItemId;
		this.menuItemId = menuItemId;
		this.menuItemName = menuItemName;
		this.unitPrice = unitPrice;
		this.quantity = quantity;
		this.lineTotal = lineTotal;
	}

	public Long getOrderItemId() {
		return orderItemId;
	}

	public Long getMenuItemId() {
		return menuItemId;
	}

	public String getMenuItemName() {
		return menuItemName;
	}

	public BigDecimal getUnitPrice() {
		return unitPrice;
	}

	public Integer getQuantity() {
		return quantity;
	}

	public BigDecimal getLineTotal() {
		return lineTotal;
	}
}