package bg.martinandonov.restaurant.kitchen.websocket.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import bg.martinandonov.restaurant.kitchen.websocket.config.KitchenWebSocketConfig;
import bg.martinandonov.restaurant.kitchen.websocket.dto.OrderRealtimeMessage;
import bg.martinandonov.restaurant.kitchen.websocket.event.OrderCreatedRealtimeEvent;
import bg.martinandonov.restaurant.kitchen.websocket.event.OrderStatusChangedRealtimeEvent;

@Component
public class OrderRealtimeNotificationListener {

	private static final Logger log = LoggerFactory.getLogger(OrderRealtimeNotificationListener.class);

	private final SimpMessagingTemplate messagingTemplate;

	public OrderRealtimeNotificationListener(SimpMessagingTemplate messagingTemplate) {
		this.messagingTemplate = messagingTemplate;
	}

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = false)
	public void onOrderCreated(OrderCreatedRealtimeEvent event) {
		dispatch(KitchenWebSocketConfig.KITCHEN_ORDERS_TOPIC, event.getMessage());
	}

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = false)
	public void onOrderStatusChanged(OrderStatusChangedRealtimeEvent event) {
		OrderRealtimeMessage message = event.getMessage();
		dispatch(KitchenWebSocketConfig.KITCHEN_ORDERS_TOPIC, message);
		dispatch(KitchenWebSocketConfig.WAITER_ORDERS_TOPIC, message);
	}

	private void dispatch(String destination, OrderRealtimeMessage message) {
		try {
			log.debug(
					"Dispatching realtime event eventId={} eventType={} orderId={} destination={}",
					message.getEventId(),
					message.getEventType(),
					message.getOrder().getId(),
					destination);
			messagingTemplate.convertAndSend(destination, message);
		}
		catch (MessagingException ex) {
			log.warn(
					"Failed to deliver realtime notification eventId={} orderId={} destination={}: {}",
					message.getEventId(),
					message.getOrder().getId(),
					destination,
					ex.getMessage());
		}
	}
}