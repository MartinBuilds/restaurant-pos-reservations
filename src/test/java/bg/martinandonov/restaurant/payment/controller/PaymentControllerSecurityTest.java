package bg.martinandonov.restaurant.payment.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import bg.martinandonov.restaurant.common.exception.BusinessRuleException;
import bg.martinandonov.restaurant.common.exception.GlobalExceptionHandler;
import bg.martinandonov.restaurant.common.exception.InvalidRequestException;
import bg.martinandonov.restaurant.common.exception.ResourceNotFoundException;
import bg.martinandonov.restaurant.payment.dto.PaymentReceiptItemResponse;
import bg.martinandonov.restaurant.payment.dto.PaymentResponse;
import bg.martinandonov.restaurant.payment.service.PaymentService;
import bg.martinandonov.restaurant.security.SecurityConfig;

@WebMvcTest(controllers = { WaiterPaymentController.class, AdminPaymentController.class })
@Import({ SecurityConfig.class, GlobalExceptionHandler.class })
class PaymentControllerSecurityTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private PaymentService paymentService;

	@MockitoBean
	private UserDetailsService userDetailsService;

	@Test
	@WithAnonymousUser
	void anonymousDenied() throws Exception {
		mockMvc.perform(post("/api/waiter/orders/1/payment")
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"method\":\"CASH\"}"))
				.andExpect(status().isUnauthorized());
		mockMvc.perform(get("/api/admin/payments"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	@WithMockUser(roles = "CLIENT")
	void clientDenied() throws Exception {
		mockMvc.perform(post("/api/waiter/orders/1/payment")
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"method\":\"CASH\"}"))
				.andExpect(status().isForbidden());
		mockMvc.perform(get("/api/admin/payments"))
				.andExpect(status().isForbidden());
	}

	@Test
	@WithMockUser(roles = "COOK")
	void cookDenied() throws Exception {
		mockMvc.perform(post("/api/waiter/orders/1/payment")
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"method\":\"CASH\"}"))
				.andExpect(status().isForbidden());
		mockMvc.perform(get("/api/admin/payments"))
				.andExpect(status().isForbidden());
	}

	@Test
	@WithMockUser(roles = "WAITER")
	void waiterCanProcessAndReadPayment() throws Exception {
		when(paymentService.processPayment(eq(1L), any())).thenReturn(sampleResponse());
		when(paymentService.getPaymentForOrder(1L)).thenReturn(sampleResponse());

		mockMvc.perform(post("/api/waiter/orders/1/payment")
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"method\":\"CASH\"}"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.simulated").value(true))
				.andExpect(jsonPath("$.method").value("CASH"));

		mockMvc.perform(get("/api/waiter/orders/1/payment"))
				.andExpect(status().isOk());

		mockMvc.perform(get("/api/admin/payments"))
				.andExpect(status().isForbidden());
	}

	@Test
	@WithMockUser(roles = "ADMIN")
	void adminCanAccessWaiterAndAdminEndpoints() throws Exception {
		when(paymentService.processPayment(eq(1L), any())).thenReturn(sampleResponse());
		when(paymentService.getPaymentsForAdmin(isNull(), isNull(), isNull(), isNull()))
				.thenReturn(List.of(sampleResponse()));
		when(paymentService.getPaymentByIdForAdmin(9L)).thenReturn(sampleResponse());

		mockMvc.perform(post("/api/waiter/orders/1/payment")
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"method\":\"CARD\"}"))
				.andExpect(status().isCreated());

		mockMvc.perform(get("/api/admin/payments"))
				.andExpect(status().isOk());
		mockMvc.perform(get("/api/admin/payments/9"))
				.andExpect(status().isOk());
	}

	@Test
	@WithMockUser(roles = "WAITER")
	void httpErrorMapping() throws Exception {
		when(paymentService.processPayment(eq(1L), any()))
				.thenThrow(new InvalidRequestException("method must be provided"));
		mockMvc.perform(post("/api/waiter/orders/1/payment")
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{}"))
				.andExpect(status().isBadRequest());

		when(paymentService.processPayment(eq(2L), any()))
				.thenThrow(new ResourceNotFoundException("Order not found: 2"));
		mockMvc.perform(post("/api/waiter/orders/2/payment")
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"method\":\"CASH\"}"))
				.andExpect(status().isNotFound());

		when(paymentService.processPayment(eq(3L), any()))
				.thenThrow(new BusinessRuleException("Only SERVED orders can be paid"));
		mockMvc.perform(post("/api/waiter/orders/3/payment")
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"method\":\"CASH\"}"))
				.andExpect(status().isConflict());

		when(paymentService.getPaymentForOrder(4L))
				.thenThrow(new ResourceNotFoundException("Payment not found for order: 4"));
		mockMvc.perform(get("/api/waiter/orders/4/payment"))
				.andExpect(status().isNotFound());
	}

	private PaymentResponse sampleResponse() {
		return new PaymentResponse(
				9L,
				"SIM-abc",
				true,
				1L,
				"ORD-1",
				3L,
				7,
				"SERVED",
				true,
				"CASH",
				new BigDecimal("25.50"),
				5L,
				"Waiter One",
				LocalDateTime.of(2026, 8, 5, 15, 0),
				List.of(new PaymentReceiptItemResponse(
						100L, 20L, "Soup Snapshot", new BigDecimal("12.75"), 2, new BigDecimal("25.50"))));
	}
}