package bg.martinandonov.restaurant.order.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import bg.martinandonov.restaurant.common.exception.BusinessRuleException;
import bg.martinandonov.restaurant.common.exception.InvalidRequestException;
import bg.martinandonov.restaurant.common.exception.ResourceNotFoundException;
import bg.martinandonov.restaurant.diningtable.entity.DiningTable;
import bg.martinandonov.restaurant.diningtable.entity.DiningTableStatus;
import bg.martinandonov.restaurant.kitchen.websocket.dto.OrderRealtimeEventType;
import bg.martinandonov.restaurant.kitchen.websocket.event.OrderStatusChangedRealtimeEvent;
import bg.martinandonov.restaurant.menu.entity.MenuCategory;
import bg.martinandonov.restaurant.menu.entity.MenuItem;
import bg.martinandonov.restaurant.order.dto.KitchenOrderResponse;
import bg.martinandonov.restaurant.order.dto.OrderResponse;
import bg.martinandonov.restaurant.order.dto.UpdateOrderStatusRequest;
import bg.martinandonov.restaurant.order.entity.OrderItem;
import bg.martinandonov.restaurant.order.entity.OrderStatus;
import bg.martinandonov.restaurant.order.entity.RestaurantOrder;
import bg.martinandonov.restaurant.order.repository.OrderItemRepository;
import bg.martinandonov.restaurant.order.repository.RestaurantOrderRepository;
import bg.martinandonov.restaurant.user.entity.AppUser;

@ExtendWith(MockitoExtension.class)
class OrderWorkflowServiceTest {

	@Mock
	private RestaurantOrderRepository restaurantOrderRepository;

	@Mock
	private OrderItemRepository orderItemRepository;

	@Mock
	private ApplicationEventPublisher applicationEventPublisher;

	@InjectMocks
	private OrderWorkflowService orderWorkflowService;

	private DiningTable table;
	private AppUser waiter;
	private MenuItem menuItem;
	private RestaurantOrder order;

	@BeforeEach
	void setUp() {
		table = new DiningTable(5, "Window", 4);
		ReflectionTestUtils.setField(table, "id", 1L);
		table.setStatus(DiningTableStatus.OCCUPIED);

		waiter = new AppUser("waiter@example.com", "hash", "Ada Waiter", true);
		ReflectionTestUtils.setField(waiter, "id", 2L);

		MenuCategory category = new MenuCategory("Salads", null, true);
		ReflectionTestUtils.setField(category, "id", 1L);
		menuItem = new MenuItem("Salad", null, new BigDecimal("12.50"), true, true, category);
		ReflectionTestUtils.setField(menuItem, "id", 10L);

		order = new RestaurantOrder("ord-1", table, waiter, LocalDateTime.of(2026, 8, 5, 1, 0));
		ReflectionTestUtils.setField(order, "id", 100L);
		ReflectionTestUtils.setField(order, "version", 0L);
	}

	@Test
	void acceptedToCookingWorks() {
		stubOrderForUpdate();
		stubItems();

		KitchenOrderResponse response = orderWorkflowService.updateFromKitchen(100L, statusRequest("COOKING"));

		assertThat(response.getStatus()).isEqualTo("COOKING");
		assertThat(order.getStatus()).isEqualTo(OrderStatus.COOKING);
		assertThat(order.isClosed()).isFalse();
		assertThat(table.getStatus()).isEqualTo(DiningTableStatus.OCCUPIED);
		assertStatusEvent(OrderStatus.ACCEPTED, OrderStatus.COOKING);
	}

	@Test
	void cookingToReadyWorks() {
		order.setStatus(OrderStatus.COOKING);
		stubOrderForUpdate();
		stubItems();

		KitchenOrderResponse response = orderWorkflowService.updateFromKitchen(100L, statusRequest("READY"));

		assertThat(response.getStatus()).isEqualTo("READY");
		assertStatusEvent(OrderStatus.COOKING, OrderStatus.READY);
	}

	@Test
	void readyToServedWorks() {
		order.setStatus(OrderStatus.READY);
		stubOrderForUpdate();
		stubItems();

		OrderResponse response = orderWorkflowService.markServedByWaiter(100L);

		assertThat(response.getStatus()).isEqualTo("SERVED");
		assertThat(response.isClosed()).isFalse();
		assertThat(table.getStatus()).isEqualTo(DiningTableStatus.OCCUPIED);
		assertThat(order.getTotalAmount()).isEqualByComparingTo("0.00");
		assertStatusEvent(OrderStatus.READY, OrderStatus.SERVED);
	}

	@Test
	void sameStatusIsIdempotent() {
		order.setStatus(OrderStatus.COOKING);
		LocalDateTime updatedAt = order.getUpdatedAt();
		stubOrderForUpdate();
		stubItems();

		KitchenOrderResponse response = orderWorkflowService.updateFromKitchen(100L, statusRequest("COOKING"));

		assertThat(response.getStatus()).isEqualTo("COOKING");
		assertThat(order.getUpdatedAt()).isEqualTo(updatedAt);
		verify(applicationEventPublisher, never()).publishEvent(any());
	}

	@Test
	void servedIdempotent() {
		order.setStatus(OrderStatus.SERVED);
		stubOrderForUpdate();
		stubItems();

		OrderResponse response = orderWorkflowService.markServedByWaiter(100L);

		assertThat(response.getStatus()).isEqualTo("SERVED");
		verify(applicationEventPublisher, never()).publishEvent(any());
	}

	@Test
	void invalidTransitionsRejected() {
		stubOrderForUpdate();
		assertThatThrownBy(() -> orderWorkflowService.updateFromKitchen(100L, statusRequest("READY")))
				.isInstanceOf(BusinessRuleException.class);

		order.setStatus(OrderStatus.COOKING);
		assertThatThrownBy(() -> orderWorkflowService.markServedByWaiter(100L))
				.isInstanceOf(BusinessRuleException.class);

		order.setStatus(OrderStatus.READY);
		assertThatThrownBy(() -> orderWorkflowService.updateFromKitchen(100L, statusRequest("COOKING")))
				.isInstanceOf(BusinessRuleException.class);

		order.setStatus(OrderStatus.SERVED);
		assertThatThrownBy(() -> orderWorkflowService.updateFromKitchen(100L, statusRequest("COOKING")))
				.isInstanceOf(BusinessRuleException.class);

		order.setStatus(OrderStatus.CANCELLED);
		assertThatThrownBy(() -> orderWorkflowService.updateFromKitchen(100L, statusRequest("COOKING")))
				.isInstanceOf(BusinessRuleException.class);

		verify(applicationEventPublisher, never()).publishEvent(any());
	}

	@Test
	void kitchenRejectsServedRequest() {
		assertThatThrownBy(() -> orderWorkflowService.updateFromKitchen(100L, statusRequest("SERVED")))
				.isInstanceOf(InvalidRequestException.class)
				.hasMessageContaining("COOKING or READY");
		verify(restaurantOrderRepository, never()).findByIdForUpdate(any());
		verify(applicationEventPublisher, never()).publishEvent(any());
	}

	@Test
	void closedOrderRejected() {
		order.setClosed(true);
		stubOrderForUpdate();

		assertThatThrownBy(() -> orderWorkflowService.updateFromKitchen(100L, statusRequest("COOKING")))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("closed");
		verify(applicationEventPublisher, never()).publishEvent(any());
	}

	@Test
	void missingOrderReturns404() {
		when(restaurantOrderRepository.findByIdForUpdate(99L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> orderWorkflowService.markServedByWaiter(99L))
				.isInstanceOf(ResourceNotFoundException.class);
		verify(applicationEventPublisher, never()).publishEvent(any());
	}

	@Test
	void kitchenQueueFiltersStatuses() {
		when(restaurantOrderRepository.findActiveKitchenOrders(
				eq(EnumSet.of(OrderStatus.ACCEPTED, OrderStatus.COOKING, OrderStatus.READY))))
				.thenReturn(List.of(order));
		stubItems();

		List<KitchenOrderResponse> responses = orderWorkflowService.getKitchenOrders(null);

		assertThat(responses).hasSize(1);
		assertThat(responses.get(0).getStatus()).isEqualTo("ACCEPTED");
	}

	@Test
	void unsupportedKitchenFilterRejected() {
		assertThatThrownBy(() -> orderWorkflowService.getKitchenOrders(OrderStatus.SERVED))
				.isInstanceOf(InvalidRequestException.class);
		assertThatThrownBy(() -> orderWorkflowService.getKitchenOrders(OrderStatus.CANCELLED))
				.isInstanceOf(InvalidRequestException.class);
	}

	@Test
	void kitchenGetByIdHidesServedClosedCancelled() {
		order.setStatus(OrderStatus.SERVED);
		when(restaurantOrderRepository.findByIdWithDetails(100L)).thenReturn(Optional.of(order));

		assertThatThrownBy(() -> orderWorkflowService.getKitchenOrderById(100L))
				.isInstanceOf(ResourceNotFoundException.class);

		order.setStatus(OrderStatus.ACCEPTED);
		order.setClosed(true);
		assertThatThrownBy(() -> orderWorkflowService.getKitchenOrderById(100L))
				.isInstanceOf(ResourceNotFoundException.class);

		order.setClosed(false);
		order.setStatus(OrderStatus.CANCELLED);
		assertThatThrownBy(() -> orderWorkflowService.getKitchenOrderById(100L))
				.isInstanceOf(ResourceNotFoundException.class);
	}

	private void stubOrderForUpdate() {
		when(restaurantOrderRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(order));
	}

	private void stubItems() {
		OrderItem item = new OrderItem(
				order,
				menuItem,
				"Salad",
				new BigDecimal("12.50"),
				1,
				new BigDecimal("12.50"));
		ReflectionTestUtils.setField(item, "id", 50L);
		when(orderItemRepository.findByOrderIdOrderByIdAsc(100L)).thenReturn(List.of(item));
	}

	private void assertStatusEvent(OrderStatus previous, OrderStatus current) {
		ArgumentCaptor<OrderStatusChangedRealtimeEvent> eventCaptor =
				ArgumentCaptor.forClass(OrderStatusChangedRealtimeEvent.class);
		verify(applicationEventPublisher).publishEvent(eventCaptor.capture());
		assertThat(eventCaptor.getValue().getMessage().getEventType())
				.isEqualTo(OrderRealtimeEventType.ORDER_STATUS_CHANGED);
		assertThat(eventCaptor.getValue().getMessage().getPreviousStatus()).isEqualTo(previous);
		assertThat(eventCaptor.getValue().getMessage().getCurrentStatus()).isEqualTo(current);
		assertThat(eventCaptor.getValue().getMessage().getOrder().getId()).isEqualTo(100L);
		assertThat(eventCaptor.getValue().getMessage().getEventId()).isNotNull();
	}

	private UpdateOrderStatusRequest statusRequest(String status) {
		UpdateOrderStatusRequest request = new UpdateOrderStatusRequest();
		request.setStatus(status);
		return request;
	}
}