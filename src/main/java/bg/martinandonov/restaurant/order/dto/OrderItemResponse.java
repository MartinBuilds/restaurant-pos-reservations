package bg.martinandonov.restaurant.order.dto;

import java.math.BigDecimal;

public class OrderItemResponse {

	private final Long id;
	private final Long menuItemId;
	private final String menuItemName;
	private final BigDecimal unitPrice;
	private final Integer quantity;
	private final BigDecimal lineTotal;

	public OrderItemResponse(
			Long id,
			Long menuItemId,
			String menuItemName,
			BigDecimal unitPrice,
			Integer quantity,
			BigDecimal lineTotal) {
		this.id = id;
		this.menuItemId = menuItemId;
		this.menuItemName = menuItemName;
		this.unitPrice = unitPrice;
		this.quantity = quantity;
		this.lineTotal = lineTotal;
	}

	public Long getId() {
		return id;
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
