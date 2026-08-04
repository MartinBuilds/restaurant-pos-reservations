package bg.martinandonov.restaurant.payment.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class PaymentResponse {

	private final Long id;
	private final String receiptNumber;
	private final boolean simulated;
	private final Long orderId;
	private final String orderNumber;
	private final Long diningTableId;
	private final Integer tableNumber;
	private final String orderStatus;
	private final boolean orderClosed;
	private final String method;
	private final BigDecimal amount;
	private final Long processedById;
	private final String processedByName;
	private final LocalDateTime paidAt;
	private final List<PaymentReceiptItemResponse> items;

	public PaymentResponse(
			Long id,
			String receiptNumber,
			boolean simulated,
			Long orderId,
			String orderNumber,
			Long diningTableId,
			Integer tableNumber,
			String orderStatus,
			boolean orderClosed,
			String method,
			BigDecimal amount,
			Long processedById,
			String processedByName,
			LocalDateTime paidAt,
			List<PaymentReceiptItemResponse> items) {
		this.id = id;
		this.receiptNumber = receiptNumber;
		this.simulated = simulated;
		this.orderId = orderId;
		this.orderNumber = orderNumber;
		this.diningTableId = diningTableId;
		this.tableNumber = tableNumber;
		this.orderStatus = orderStatus;
		this.orderClosed = orderClosed;
		this.method = method;
		this.amount = amount;
		this.processedById = processedById;
		this.processedByName = processedByName;
		this.paidAt = paidAt;
		this.items = items;
	}

	public Long getId() {
		return id;
	}

	public String getReceiptNumber() {
		return receiptNumber;
	}

	public boolean isSimulated() {
		return simulated;
	}

	public Long getOrderId() {
		return orderId;
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

	public String getOrderStatus() {
		return orderStatus;
	}

	public boolean isOrderClosed() {
		return orderClosed;
	}

	public String getMethod() {
		return method;
	}

	public BigDecimal getAmount() {
		return amount;
	}

	public Long getProcessedById() {
		return processedById;
	}

	public String getProcessedByName() {
		return processedByName;
	}

	public LocalDateTime getPaidAt() {
		return paidAt;
	}

	public List<PaymentReceiptItemResponse> getItems() {
		return items;
	}
}