package bg.martinandonov.restaurant.order.dto;

import java.time.LocalDateTime;
import java.util.List;

public class KitchenOrderResponse {

	private final Long id;
	private final String orderNumber;
	private final Long diningTableId;
	private final Integer tableNumber;
	private final String status;
	private final LocalDateTime createdAt;
	private final LocalDateTime updatedAt;
	private final List<KitchenOrderItemResponse> items;

	public KitchenOrderResponse(
			Long id,
			String orderNumber,
			Long diningTableId,
			Integer tableNumber,
			String status,
			LocalDateTime createdAt,
			LocalDateTime updatedAt,
			List<KitchenOrderItemResponse> items) {
		this.id = id;
		this.orderNumber = orderNumber;
		this.diningTableId = diningTableId;
		this.tableNumber = tableNumber;
		this.status = status;
		this.createdAt = createdAt;
		this.updatedAt = updatedAt;
		this.items = items;
	}

	public Long getId() {
		return id;
	}

	public String getOrderNumber() {
		return orderNumber;
	}

	public Long getDiningTableId() {
		return diningTableId;
	}

	public Integer getTableNumber() {
		return tableNumber;
	}

	public String getStatus() {
		return status;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public LocalDateTime getUpdatedAt() {
		return updatedAt;
	}

	public List<KitchenOrderItemResponse> getItems() {
		return items;
	}
}
