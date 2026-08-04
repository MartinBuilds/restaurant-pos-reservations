package bg.martinandonov.restaurant.order.service;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import bg.martinandonov.restaurant.common.exception.BusinessRuleException;
import bg.martinandonov.restaurant.common.exception.InvalidRequestException;
import bg.martinandonov.restaurant.common.exception.ResourceNotFoundException;
import bg.martinandonov.restaurant.kitchen.websocket.dto.OrderRealtimeMessage;
import bg.martinandonov.restaurant.kitchen.websocket.event.OrderStatusChangedRealtimeEvent;
import bg.martinandonov.restaurant.order.dto.KitchenOrderItemResponse;
import bg.martinandonov.restaurant.order.dto.KitchenOrderResponse;
import bg.martinandonov.restaurant.order.dto.OrderItemResponse;
import bg.martinandonov.restaurant.order.dto.OrderResponse;
import bg.martinandonov.restaurant.order.dto.UpdateOrderStatusRequest;
import bg.martinandonov.restaurant.order.entity.OrderItem;
import bg.martinandonov.restaurant.order.entity.OrderStatus;
import bg.martinandonov.restaurant.order.entity.RestaurantOrder;
import bg.martinandonov.restaurant.order.repository.OrderItemRepository;
import bg.martinandonov.restaurant.order.repository.RestaurantOrderRepository;

@Service
@Transactional
public class OrderWorkflowService {

	private static final Set<OrderStatus> KITCHEN_QUEUE_STATUSES =
			EnumSet.of(OrderStatus.ACCEPTED, OrderStatus.COOKING, OrderStatus.READY);

	private final RestaurantOrderRepository restaurantOrderRepository;
	private final OrderItemRepository orderItemRepository;
	private final ApplicationEventPublisher applicationEventPublisher;

	public OrderWorkflowService(
			RestaurantOrderRepository restaurantOrderRepository,
			OrderItemRepository orderItemRepository,
			ApplicationEventPublisher applicationEventPublisher) {
		this.restaurantOrderRepository = restaurantOrderRepository;
		this.orderItemRepository = orderItemRepository;
		this.applicationEventPublisher = applicationEventPublisher;
	}

	public KitchenOrderResponse updateFromKitchen(Long orderId, UpdateOrderStatusRequest request) {
		Objects.requireNonNull(request, "request must not be null");
		OrderStatus requested = requireStatus(request.getStatus());
		if (requested != OrderStatus.COOKING && requested != OrderStatus.READY) {
			throw new InvalidRequestException("Kitchen can only set COOKING or READY");
		}

		RestaurantOrder order = loadOrderForUpdate(orderId);
		assertOrderOpen(order);
		if (order.getStatus() == requested) {
			return toKitchenResponse(order);
		}
		OrderStatus previousStatus = order.getStatus();
		assertValidTransition(previousStatus, requested);
		order.setStatus(requested);
		order.setUpdatedAt(LocalDateTime.now());
		KitchenOrderResponse kitchenSnapshot = toKitchenResponse(order);
		publishStatusChanged(previousStatus, requested, kitchenSnapshot);
		return kitchenSnapshot;
	}

	public OrderResponse markServedByWaiter(Long orderId) {
		RestaurantOrder order = loadOrderForUpdate(orderId);
		assertOrderOpen(order);
		if (order.getStatus() == OrderStatus.SERVED) {
			return toWaiterResponse(order);
		}
		OrderStatus previousStatus = order.getStatus();
		assertValidTransition(previousStatus, OrderStatus.SERVED);
		order.setStatus(OrderStatus.SERVED);
		order.setUpdatedAt(LocalDateTime.now());
		publishStatusChanged(previousStatus, OrderStatus.SERVED, toKitchenResponse(order));
		return toWaiterResponse(order);
	}

	@Transactional(readOnly = true)
	public List<KitchenOrderResponse> getKitchenOrders(OrderStatus optionalStatus) {
		List<RestaurantOrder> orders;
		if (optionalStatus == null) {
			orders = restaurantOrderRepository.findActiveKitchenOrders(KITCHEN_QUEUE_STATUSES);
		}
		else {
			if (!KITCHEN_QUEUE_STATUSES.contains(optionalStatus)) {
				throw new InvalidRequestException(
						"Kitchen status filter must be ACCEPTED, COOKING or READY");
			}
			orders = restaurantOrderRepository.findActiveKitchenOrdersByStatus(optionalStatus);
		}
		return orders.stream().map(this::toKitchenResponse).toList();
	}

	@Transactional(readOnly = true)
	public KitchenOrderResponse getKitchenOrderById(Long orderId) {
		RestaurantOrder order = restaurantOrderRepository.findByIdWithDetails(orderId)
				.orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));
		if (order.isClosed() || !KITCHEN_QUEUE_STATUSES.contains(order.getStatus())) {
			throw new ResourceNotFoundException("Order not found: " + orderId);
		}
		return toKitchenResponse(order);
	}

	private RestaurantOrder loadOrderForUpdate(Long orderId) {
		if (orderId == null) {
			throw new InvalidRequestException("Order id must be provided");
		}
		return restaurantOrderRepository.findByIdForUpdate(orderId)
				.orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));
	}

	private void assertOrderOpen(RestaurantOrder order) {
		if (order.isClosed()) {
			throw new BusinessRuleException("Cannot update status of a closed order");
		}
	}

	private void assertValidTransition(OrderStatus current, OrderStatus requested) {
		boolean allowed = (current == OrderStatus.ACCEPTED && requested == OrderStatus.COOKING)
				|| (current == OrderStatus.COOKING && requested == OrderStatus.READY)
				|| (current == OrderStatus.READY && requested == OrderStatus.SERVED);
		if (!allowed) {
			throw new BusinessRuleException(
					"Invalid order status transition from " + current + " to " + requested);
		}
	}

	private OrderStatus requireStatus(String status) {
		if (status == null || status.isBlank()) {
			throw new InvalidRequestException("status must be provided");
		}
		try {
			return OrderStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
		}
		catch (IllegalArgumentException ex) {
			throw new InvalidRequestException("Unknown order status: " + status.trim());
		}
	}

	private KitchenOrderResponse toKitchenResponse(RestaurantOrder order) {
		List<KitchenOrderItemResponse> items = orderItemRepository.findByOrderIdOrderByIdAsc(order.getId())
				.stream()
				.map(this::toKitchenItem)
				.toList();
		return new KitchenOrderResponse(
				order.getId(),
				order.getOrderNumber(),
				order.getDiningTable().getId(),
				order.getDiningTable().getTableNumber(),
				order.getStatus().name(),
				order.getCreatedAt(),
				order.getUpdatedAt(),
				items);
	}

	private KitchenOrderItemResponse toKitchenItem(OrderItem item) {
		return new KitchenOrderItemResponse(
				item.getId(),
				item.getMenuItem().getId(),
				item.getMenuItemName(),
				item.getQuantity());
	}

	private void publishStatusChanged(
			OrderStatus previousStatus,
			OrderStatus currentStatus,
			KitchenOrderResponse kitchenSnapshot) {
		applicationEventPublisher.publishEvent(new OrderStatusChangedRealtimeEvent(
				OrderRealtimeMessage.statusChanged(previousStatus, currentStatus, kitchenSnapshot)));
	}

	private OrderResponse toWaiterResponse(RestaurantOrder order) {
		List<OrderItemResponse> items = orderItemRepository.findByOrderIdOrderByIdAsc(order.getId()).stream()
				.map(item -> new OrderItemResponse(
						item.getId(),
						item.getMenuItem().getId(),
						item.getMenuItemName(),
						item.getUnitPrice(),
						item.getQuantity(),
						item.getLineTotal()))
				.toList();
		return new OrderResponse(
				order.getId(),
				order.getOrderNumber(),
				order.getDiningTable().getId(),
				order.getDiningTable().getTableNumber(),
				order.getWaiter().getId(),
				order.getWaiter().getFullName(),
				order.getStatus().name(),
				order.isClosed(),
				order.getTotalAmount(),
				order.getCreatedAt(),
				order.getUpdatedAt(),
				items);
	}
}