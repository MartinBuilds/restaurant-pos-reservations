package bg.martinandonov.restaurant.payment.controller;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import bg.martinandonov.restaurant.payment.dto.PaymentResponse;
import bg.martinandonov.restaurant.payment.entity.PaymentMethod;
import bg.martinandonov.restaurant.payment.service.PaymentService;

@RestController
@RequestMapping("/api/admin/payments")
public class AdminPaymentController {

	private final PaymentService paymentService;

	public AdminPaymentController(PaymentService paymentService) {
		this.paymentService = paymentService;
	}

	@GetMapping
	public ResponseEntity<List<PaymentResponse>> list(
			@RequestParam(required = false) String method,
			@RequestParam(required = false) LocalDateTime from,
			@RequestParam(required = false) LocalDateTime to,
			@RequestParam(required = false) Long processedById) {
		PaymentMethod paymentMethod = PaymentService.parseOptionalMethod(method);
		return ResponseEntity.ok(paymentService.getPaymentsForAdmin(paymentMethod, from, to, processedById));
	}

	@GetMapping("/{id}")
	public ResponseEntity<PaymentResponse> getById(@PathVariable Long id) {
		return ResponseEntity.ok(paymentService.getPaymentByIdForAdmin(id));
	}
}