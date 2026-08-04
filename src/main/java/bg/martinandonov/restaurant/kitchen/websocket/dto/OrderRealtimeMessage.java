package bg.martinandonov.restaurant.kitchen.websocket.dto;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import bg.martinandonov.restaurant.order.dto.KitchenOrderResponse;
import bg.martinandonov.restaurant.order.entity.OrderStatus;

public final class OrderRealtimeMessage {

	private final UUID eventId;
	private final OrderRealtimeEventType eventType;
	private final Instant occurredAt;
	private final OrderStatus previousStatus;
	private final OrderStatus currentStatus;
	private final KitchenOrderResponse order;

	public OrderRealtimeMessage(
			UUID eventId,
			OrderRealtimeEventType eventType,
			Instant occurredAt,
			OrderStatus previousStatus,
			OrderStatus currentStatus,
			KitchenOrderResponse order) {
		this.eventId = Objects.requireNonNull(eventId, "eventId must not be null");
		this.eventType = Objects.requireNonNull(eventType, "eventType must not be null");
		this.occurredAt = Objects.requireNonNull(occurredAt, "occurredAt must not be null");
		this.previousStatus = previousStatus;
		this.currentStatus = Objects.requireNonNull(currentStatus, "currentStatus must not be null");
		this.order = Objects.requireNonNull(order, "order must not be null");
	}

	public static OrderRealtimeMessage orderCreated(KitchenOrderResponse order) {
		return new OrderRealtimeMessage(
				UUID.randomUUID(),
				OrderRealtimeEventType.ORDER_CREATED,
				Instant.now(),
				null,
				OrderStatus.ACCEPTED,
				order);
	}

	public static OrderRealtimeMessage statusChanged(
			OrderStatus previousStatus,
			OrderStatus currentStatus,
			KitchenOrderResponse order) {
		return new OrderRealtimeMessage(
				UUID.randomUUID(),
				OrderRealtimeEventType.ORDER_STATUS_CHANGED,
				Instant.now(),
				previousStatus,
				currentStatus,
				order);
	}

	public UUID getEventId() {
		return eventId;
	}

	public OrderRealtimeEventType getEventType() {
		return eventType;
	}

	public Instant getOccurredAt() {
		return occurredAt;
	}

	public OrderStatus getPreviousStatus() {
		return previousStatus;
	}

	public OrderStatus getCurrentStatus() {
		return currentStatus;
	}

	public KitchenOrderResponse getOrder() {
		return order;
	}
}