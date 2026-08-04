package bg.martinandonov.restaurant.order.controller;

import java.util.List;
import java.util.Locale;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import bg.martinandonov.restaurant.common.exception.InvalidRequestException;
import bg.martinandonov.restaurant.order.dto.KitchenOrderResponse;
import bg.martinandonov.restaurant.order.dto.UpdateOrderStatusRequest;
import bg.martinandonov.restaurant.order.entity.OrderStatus;
import bg.martinandonov.restaurant.order.service.OrderWorkflowService;

@RestController
@RequestMapping("/api/kitchen/orders")
public class KitchenOrderController {

	private final OrderWorkflowService orderWorkflowService;

	public KitchenOrderController(OrderWorkflowService orderWorkflowService) {
		this.orderWorkflowService = orderWorkflowService;
	}

	@GetMapping
	public ResponseEntity<List<KitchenOrderResponse>> getKitchenOrders(
			@RequestParam(required = false) String status) {
		OrderStatus filter = null;
		if (status != null && !status.isBlank()) {
			filter = parseStatus(status);
		}
		return ResponseEntity.ok(orderWorkflowService.getKitchenOrders(filter));
	}

	@GetMapping("/{id}")
	public ResponseEntity<KitchenOrderResponse> getKitchenOrderById(@PathVariable Long id) {
		return ResponseEntity.ok(orderWorkflowService.getKitchenOrderById(id));
	}

	@PatchMapping("/{id}/status")
	public ResponseEntity<KitchenOrderResponse> updateStatus(
			@PathVariable Long id,
			@RequestBody UpdateOrderStatusRequest request) {
		return ResponseEntity.ok(orderWorkflowService.updateFromKitchen(id, request));
	}

	private OrderStatus parseStatus(String status) {
		try {
			return OrderStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
		}
		catch (IllegalArgumentException ex) {
			throw new InvalidRequestException("Unknown order status: " + status.trim());
		}
	}
}
