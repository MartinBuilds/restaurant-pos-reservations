package bg.martinandonov.restaurant.report.controller;

import java.time.LocalDateTime;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import bg.martinandonov.restaurant.report.dto.MenuItemSalesReportResponse;
import bg.martinandonov.restaurant.report.dto.PaymentMethodSalesReportResponse;
import bg.martinandonov.restaurant.report.dto.SalesSummaryResponse;
import bg.martinandonov.restaurant.report.service.SalesReportService;

@RestController
@RequestMapping("/api/admin/reports/sales")
public class AdminSalesReportController {

	private final SalesReportService salesReportService;

	public AdminSalesReportController(SalesReportService salesReportService) {
		this.salesReportService = salesReportService;
	}

	@GetMapping("/summary")
	public ResponseEntity<SalesSummaryResponse> summary(
			@RequestParam(required = false) LocalDateTime from,
			@RequestParam(required = false) LocalDateTime to) {
		return ResponseEntity.ok(salesReportService.getSalesSummary(from, to));
	}

	@GetMapping("/by-item")
	public ResponseEntity<MenuItemSalesReportResponse> byItem(
			@RequestParam(required = false) LocalDateTime from,
			@RequestParam(required = false) LocalDateTime to) {
		return ResponseEntity.ok(salesReportService.getSalesByMenuItem(from, to));
	}

	@GetMapping("/by-payment-method")
	public ResponseEntity<PaymentMethodSalesReportResponse> byPaymentMethod(
			@RequestParam(required = false) LocalDateTime from,
			@RequestParam(required = false) LocalDateTime to) {
		return ResponseEntity.ok(salesReportService.getSalesByPaymentMethod(from, to));
	}
}