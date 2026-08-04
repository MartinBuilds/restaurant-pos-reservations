package bg.martinandonov.restaurant.kitchen.websocket.event;

import java.util.Objects;

import bg.martinandonov.restaurant.kitchen.websocket.dto.OrderRealtimeMessage;

public final class OrderStatusChangedRealtimeEvent {

	private final OrderRealtimeMessage message;

	public OrderStatusChangedRealtimeEvent(OrderRealtimeMessage message) {
		this.message = Objects.requireNonNull(message, "message must not be null");
	}

	public OrderRealtimeMessage getMessage() {
		return message;
	}
}