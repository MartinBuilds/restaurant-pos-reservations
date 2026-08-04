package bg.martinandonov.restaurant.order.dto;

import java.util.ArrayList;
import java.util.List;

public class AddOrderItemsRequest {

	private List<CreateOrderItemRequest> items = new ArrayList<>();

	public List<CreateOrderItemRequest> getItems() {
		return items;
	}

	public void setItems(List<CreateOrderItemRequest> items) {
		this.items = items;
	}
}
