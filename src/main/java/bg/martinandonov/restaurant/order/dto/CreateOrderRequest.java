package bg.martinandonov.restaurant.order.dto;

import java.util.ArrayList;
import java.util.List;

public class CreateOrderRequest {

	private Long diningTableId;
	private List<CreateOrderItemRequest> items = new ArrayList<>();

	public Long getDiningTableId() {
		return diningTableId;
	}

	public void setDiningTableId(Long diningTableId) {
		this.diningTableId = diningTableId;
	}

	public List<CreateOrderItemRequest> getItems() {
		return items;
	}

	public void setItems(List<CreateOrderItemRequest> items) {
		this.items = items;
	}
}
