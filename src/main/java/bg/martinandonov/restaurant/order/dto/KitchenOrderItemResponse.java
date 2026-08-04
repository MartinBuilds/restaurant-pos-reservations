package bg.martinandonov.restaurant.order.dto;

public class KitchenOrderItemResponse {

	private final Long orderItemId;
	private final Long menuItemId;
	private final String menuItemName;
	private final Integer quantity;

	public KitchenOrderItemResponse(
			Long orderItemId,
			Long menuItemId,
			String menuItemName,
			Integer quantity) {
		this.orderItemId = orderItemId;
		this.menuItemId = menuItemId;
		this.menuItemName = menuItemName;
		this.quantity = quantity;
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

	public Integer getQuantity() {
		return quantity;
	}
}
