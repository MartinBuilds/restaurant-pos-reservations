package bg.martinandonov.restaurant.report.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import bg.martinandonov.restaurant.common.exception.GlobalExceptionHandler;
import bg.martinandonov.restaurant.common.exception.InvalidRequestException;
import bg.martinandonov.restaurant.payment.entity.PaymentMethod;
import bg.martinandonov.restaurant.report.dto.MenuItemSalesReportResponse;
import bg.martinandonov.restaurant.report.dto.PaymentMethodSalesReportResponse;
import bg.martinandonov.restaurant.report.dto.PaymentMethodSalesRowResponse;
import bg.martinandonov.restaurant.report.dto.ReportPeriodResponse;
import bg.martinandonov.restaurant.report.dto.SalesSummaryResponse;
import bg.martinandonov.restaurant.report.service.SalesReportService;
import bg.martinandonov.restaurant.security.SecurityConfig;

@WebMvcTest(controllers = AdminSalesReportController.class)
@Import({ SecurityConfig.class, GlobalExceptionHandler.class })
class SalesReportControllerSecurityTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private SalesReportService salesReportService;

	@MockitoBean
	private UserDetailsService userDetailsService;

	private final LocalDateTime from = LocalDateTime.of(2026, 8, 1, 0, 0);
	private final LocalDateTime to = LocalDateTime.of(2026, 8, 2, 0, 0);

	@Test
	@WithAnonymousUser
	void anonymousDenied() throws Exception {
		mockMvc.perform(get("/api/admin/reports/sales/summary")
						.param("from", "2026-08-01T00:00:00")
						.param("to", "2026-08-02T00:00:00"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	@WithMockUser(roles = "WAITER")
	void waiterDenied() throws Exception {
		mockMvc.perform(get("/api/admin/reports/sales/summary")
						.param("from", "2026-08-01T00:00:00")
						.param("to", "2026-08-02T00:00:00"))
				.andExpect(status().isForbidden());
	}

	@Test
	@WithMockUser(roles = "COOK")
	void cookDenied() throws Exception {
		mockMvc.perform(get("/api/admin/reports/sales/by-item")
						.param("from", "2026-08-01T00:00:00")
						.param("to", "2026-08-02T00:00:00"))
				.andExpect(status().isForbidden());
	}

	@Test
	@WithMockUser(roles = "CLIENT")
	void clientDenied() throws Exception {
		mockMvc.perform(get("/api/admin/reports/sales/by-payment-method")
						.param("from", "2026-08-01T00:00:00")
						.param("to", "2026-08-02T00:00:00"))
				.andExpect(status().isForbidden());
	}

	@Test
	@WithMockUser(roles = "ADMIN")
	void adminCanAccessAllReportEndpoints() throws Exception {
		ReportPeriodResponse period = new ReportPeriodResponse(from, to, "Europe/Sofia");
		when(salesReportService.getSalesSummary(any(), any())).thenReturn(
				new SalesSummaryResponse(period, new BigDecimal("0.00"), 0L, 0L, new BigDecimal("0.00")));
		when(salesReportService.getSalesByMenuItem(any(), any())).thenReturn(
				new MenuItemSalesReportResponse(period, List.of()));
		when(salesReportService.getSalesByPaymentMethod(any(), any())).thenReturn(
				new PaymentMethodSalesReportResponse(
						period,
						new BigDecimal("0.00"),
						List.of(
								new PaymentMethodSalesRowResponse(
										PaymentMethod.CASH, 0L, new BigDecimal("0.00"), new BigDecimal("0.00")),
								new PaymentMethodSalesRowResponse(
										PaymentMethod.CARD, 0L, new BigDecimal("0.00"), new BigDecimal("0.00")))));

		mockMvc.perform(get("/api/admin/reports/sales/summary")
						.param("from", "2026-08-01T00:00:00")
						.param("to", "2026-08-02T00:00:00"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.paidOrdersCount").value(0));

		mockMvc.perform(get("/api/admin/reports/sales/by-item")
						.param("from", "2026-08-01T00:00:00")
						.param("to", "2026-08-02T00:00:00"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.items").isArray());

		mockMvc.perform(get("/api/admin/reports/sales/by-payment-method")
						.param("from", "2026-08-01T00:00:00")
						.param("to", "2026-08-02T00:00:00"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.methods[0].method").value("CASH"));
	}

	@Test
	@WithMockUser(roles = "ADMIN")
	void invalidPeriodReturns400() throws Exception {
		when(salesReportService.getSalesSummary(any(), any()))
				.thenThrow(new InvalidRequestException("from must be before to"));

		mockMvc.perform(get("/api/admin/reports/sales/summary")
						.param("from", "2026-08-02T00:00:00")
						.param("to", "2026-08-01T00:00:00"))
				.andExpect(status().isBadRequest());
	}

	@Test
	@WithMockUser(roles = "ADMIN")
	void missingToReturns400() throws Exception {
		when(salesReportService.getSalesSummary(any(), any()))
				.thenThrow(new InvalidRequestException("to must be provided"));

		mockMvc.perform(get("/api/admin/reports/sales/summary")
						.param("from", "2026-08-01T00:00:00"))
				.andExpect(status().isBadRequest());
	}

	@Test
	@WithMockUser(roles = "ADMIN")
	void missingFromReturns400() throws Exception {
		when(salesReportService.getSalesSummary(any(), any()))
				.thenThrow(new InvalidRequestException("from must be provided"));

		mockMvc.perform(get("/api/admin/reports/sales/summary")
						.param("to", "2026-08-02T00:00:00"))
				.andExpect(status().isBadRequest());
	}

	@Test
	@WithMockUser(roles = "ADMIN")
	void emptyPeriodReturns200WithZeros() throws Exception {
		ReportPeriodResponse period = new ReportPeriodResponse(from, to, "Europe/Sofia");
		when(salesReportService.getSalesSummary(any(), any())).thenReturn(
				new SalesSummaryResponse(period, new BigDecimal("0.00"), 0L, 0L, new BigDecimal("0.00")));

		mockMvc.perform(get("/api/admin/reports/sales/summary")
						.param("from", "2026-08-01T00:00:00")
						.param("to", "2026-08-02T00:00:00"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalRevenue").value(0.0))
				.andExpect(jsonPath("$.paidOrdersCount").value(0));
	}
}
