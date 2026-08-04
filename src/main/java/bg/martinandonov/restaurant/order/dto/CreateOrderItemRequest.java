package bg.martinandonov.restaurant.order.dto;

public class CreateOrderItemRequest {

	private Long menuItemId;
	private Integer quantity;

	public Long getMenuItemId() {
		return menuItemId;
	}

	public void setMenuItemId(Long menuItemId) {
		this.menuItemId = menuItemId;
	}

	public Integer getQuantity() {
		return quantity;
	}

	public void setQuantity(Integer quantity) {
		this.quantity = quantity;
	}
}
