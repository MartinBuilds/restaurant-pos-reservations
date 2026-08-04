package bg.martinandonov.restaurant.payment.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import bg.martinandonov.restaurant.payment.dto.PaymentResponse;
import bg.martinandonov.restaurant.payment.dto.ProcessPaymentRequest;
import bg.martinandonov.restaurant.payment.service.PaymentService;

@RestController
@RequestMapping("/api/waiter/orders/{orderId}/payment")
public class WaiterPaymentController {

	private final PaymentService paymentService;

	public WaiterPaymentController(PaymentService paymentService) {
		this.paymentService = paymentService;
	}

	@PostMapping
	public ResponseEntity<PaymentResponse> processPayment(
			@PathVariable Long orderId,
			@RequestBody ProcessPaymentRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(paymentService.processPayment(orderId, request));
	}

	@GetMapping
	public ResponseEntity<PaymentResponse> getPayment(@PathVariable Long orderId) {
		return ResponseEntity.ok(paymentService.getPaymentForOrder(orderId));
	}
}