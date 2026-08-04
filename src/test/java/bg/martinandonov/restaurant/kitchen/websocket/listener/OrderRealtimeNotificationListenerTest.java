package bg.martinandonov.restaurant.kitchen.websocket.listener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.lang.reflect.Method;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import bg.martinandonov.restaurant.kitchen.websocket.config.KitchenWebSocketConfig;
import bg.martinandonov.restaurant.kitchen.websocket.dto.OrderRealtimeEventType;
import bg.martinandonov.restaurant.kitchen.websocket.dto.OrderRealtimeMessage;
import bg.martinandonov.restaurant.kitchen.websocket.event.OrderCreatedRealtimeEvent;
import bg.martinandonov.restaurant.kitchen.websocket.event.OrderStatusChangedRealtimeEvent;
import bg.martinandonov.restaurant.order.dto.KitchenOrderItemResponse;
import bg.martinandonov.restaurant.order.dto.KitchenOrderResponse;
import bg.martinandonov.restaurant.order.entity.OrderStatus;

@ExtendWith(MockitoExtension.class)
class OrderRealtimeNotificationListenerTest {

	@Mock
	private SimpMessagingTemplate messagingTemplate;

	@InjectMocks
	private OrderRealtimeNotificationListener listener;

	@Test
	void orderCreatedSendsOnlyKitchenTopic() {
		OrderRealtimeMessage message = sampleMessage(OrderRealtimeEventType.ORDER_CREATED, null, OrderStatus.ACCEPTED);

		listener.onOrderCreated(new OrderCreatedRealtimeEvent(message));

		verify(messagingTemplate, times(1))
				.convertAndSend(eq(KitchenWebSocketConfig.KITCHEN_ORDERS_TOPIC), same(message));
		verifyNoMoreInteractions(messagingTemplate);
	}

	@Test
	void statusChangedSendsKitchenAndWaiterWithSamePayload() {
		OrderRealtimeMessage message = sampleMessage(
				OrderRealtimeEventType.ORDER_STATUS_CHANGED,
				OrderStatus.ACCEPTED,
				OrderStatus.COOKING);

		listener.onOrderStatusChanged(new OrderStatusChangedRealtimeEvent(message));

		verify(messagingTemplate, times(1))
				.convertAndSend(eq(KitchenWebSocketConfig.KITCHEN_ORDERS_TOPIC), same(message));
		verify(messagingTemplate, times(1))
				.convertAndSend(eq(KitchenWebSocketConfig.WAITER_ORDERS_TOPIC), same(message));
		verifyNoMoreInteractions(messagingTemplate);
	}

	@Test
	void listenersUseAfterCommitWithoutFallback() throws Exception {
		assertAfterCommit(OrderRealtimeNotificationListener.class.getMethod(
				"onOrderCreated", OrderCreatedRealtimeEvent.class));
		assertAfterCommit(OrderRealtimeNotificationListener.class.getMethod(
				"onOrderStatusChanged", OrderStatusChangedRealtimeEvent.class));
	}

	@Test
	void messagingExceptionDoesNotPropagate() {
		OrderRealtimeMessage message = sampleMessage(OrderRealtimeEventType.ORDER_CREATED, null, OrderStatus.ACCEPTED);
		doThrow(new MessagingException("broker unavailable"))
				.when(messagingTemplate)
				.convertAndSend(eq(KitchenWebSocketConfig.KITCHEN_ORDERS_TOPIC), same(message));

		assertThatCode(() -> listener.onOrderCreated(new OrderCreatedRealtimeEvent(message)))
				.doesNotThrowAnyException();

		verify(messagingTemplate, times(1))
				.convertAndSend(eq(KitchenWebSocketConfig.KITCHEN_ORDERS_TOPIC), same(message));
		verifyNoMoreInteractions(messagingTemplate);
	}

	private static void assertAfterCommit(Method method) {
		TransactionalEventListener annotation = method.getAnnotation(TransactionalEventListener.class);
		assertThat(annotation).isNotNull();
		assertThat(annotation.phase()).isEqualTo(TransactionPhase.AFTER_COMMIT);
		assertThat(annotation.fallbackExecution()).isFalse();
	}

	private static OrderRealtimeMessage sampleMessage(
			OrderRealtimeEventType type,
			OrderStatus previous,
			OrderStatus current) {
		KitchenOrderResponse order = new KitchenOrderResponse(
				42L,
				"ord-42",
				1L,
				5,
				current.name(),
				LocalDateTime.of(2026, 8, 5, 1, 0),
				LocalDateTime.of(2026, 8, 5, 1, 5),
				List.of(new KitchenOrderItemResponse(1L, 10L, "Salad", 2)));
		return new OrderRealtimeMessage(
				UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee"),
				type,
				Instant.parse("2026-08-05T01:05:00Z"),
				previous,
				current,
				order);
	}
}