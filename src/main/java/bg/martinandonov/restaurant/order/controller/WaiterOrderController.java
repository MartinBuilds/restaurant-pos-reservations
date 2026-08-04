package bg.martinandonov.restaurant.order.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import bg.martinandonov.restaurant.order.dto.AddOrderItemsRequest;
import bg.martinandonov.restaurant.order.dto.CreateOrderRequest;
import bg.martinandonov.restaurant.order.dto.OrderResponse;
import bg.martinandonov.restaurant.order.service.OrderService;

@RestController
@RequestMapping("/api/waiter/orders")
public class WaiterOrderController {

	private final OrderService orderService;

	public WaiterOrderController(OrderService orderService) {
		this.orderService = orderService;
	}

	@PostMapping
	public ResponseEntity<OrderResponse> createOrder(@RequestBody CreateOrderRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(orderService.createOrder(request));
	}

	@GetMapping
	public ResponseEntity<List<OrderResponse>> getOrders(@RequestParam(required = false) Long tableId) {
		if (tableId != null) {
			return ResponseEntity.ok(orderService.getOpenOrdersByTable(tableId));
		}
		return ResponseEntity.ok(orderService.getOpenOrders());
	}

	@GetMapping("/{id}")
	public ResponseEntity<OrderResponse> getOrderById(@PathVariable Long id) {
		return ResponseEntity.ok(orderService.getOrderById(id));
	}

	@PostMapping("/{id}/items")
	public ResponseEntity<OrderResponse> addItems(
			@PathVariable Long id,
			@RequestBody AddOrderItemsRequest request) {
		return ResponseEntity.ok(orderService.addItemsToOrder(id, request));
	}
}
