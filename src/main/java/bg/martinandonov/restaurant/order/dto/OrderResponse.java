package bg.martinandonov.restaurant.order.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class OrderResponse {

	private final Long id;
	private final String orderNumber;
	private final Long diningTableId;
	private final Integer tableNumber;
	private final Long waiterId;
	private final String waiterName;
	private final String status;
	private final boolean closed;
	private final BigDecimal totalAmount;
	private final LocalDateTime createdAt;
	private final LocalDateTime updatedAt;
	private final List<OrderItemResponse> items;

	public OrderResponse(
			Long id,
			String orderNumber,
			Long diningTableId,
			Integer tableNumber,
			Long waiterId,
			String waiterName,
			String status,
			boolean closed,
			BigDecimal totalAmount,
			LocalDateTime createdAt,
			LocalDateTime updatedAt,
			List<OrderItemResponse> items) {
		this.id = id;
		this.orderNumber = orderNumber;
		this.diningTableId = diningTableId;
		this.tableNumber = tableNumber;
		this.waiterId = waiterId;
		this.waiterName = waiterName;
		this.status = status;
		this.closed = closed;
		this.totalAmount = totalAmount;
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

	public Long getWaiterId() {
		return waiterId;
	}

	public String getWaiterName() {
		return waiterName;
	}

	public String getStatus() {
		return status;
	}

	public boolean isClosed() {
		return closed;
	}

	public BigDecimal getTotalAmount() {
		return totalAmount;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public LocalDateTime getUpdatedAt() {
		return updatedAt;
	}

	public List<OrderItemResponse> getItems() {
		return items;
	}
}
