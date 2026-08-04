package bg.martinandonov.restaurant.order.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
import bg.martinandonov.restaurant.order.dto.OrderItemResponse;
import bg.martinandonov.restaurant.order.dto.OrderResponse;
import bg.martinandonov.restaurant.order.service.OrderService;
import bg.martinandonov.restaurant.order.service.OrderWorkflowService;
import bg.martinandonov.restaurant.security.SecurityConfig;

@WebMvcTest(controllers = WaiterOrderController.class)
@Import({ SecurityConfig.class, GlobalExceptionHandler.class })
class OrderControllerSecurityTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private OrderService orderService;

	@MockitoBean
	private OrderWorkflowService orderWorkflowService;

	@MockitoBean
	private UserDetailsService userDetailsService;

	@Test
	@WithAnonymousUser
	void anonymousCannotAccessOrders() throws Exception {
		mockMvc.perform(get("/api/waiter/orders"))
				.andExpect(status().isUnauthorized());
		mockMvc.perform(post("/api/waiter/orders")
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "diningTableId": 1,
								  "items": [{ "menuItemId": 10, "quantity": 1 }]
								}
								"""))
				.andExpect(status().isUnauthorized());
	}

	@Test
	@WithMockUser(roles = "CLIENT")
	void clientCannotAccessOrders() throws Exception {
		mockMvc.perform(get("/api/waiter/orders"))
				.andExpect(status().isForbidden());
		mockMvc.perform(post("/api/waiter/orders")
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "diningTableId": 1,
								  "items": [{ "menuItemId": 10, "quantity": 1 }]
								}
								"""))
				.andExpect(status().isForbidden());
	}

	@Test
	@WithMockUser(roles = "COOK")
	void cookCannotAccessOrders() throws Exception {
		mockMvc.perform(get("/api/waiter/orders"))
				.andExpect(status().isForbidden());
		mockMvc.perform(post("/api/waiter/orders")
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "diningTableId": 1,
								  "items": [{ "menuItemId": 10, "quantity": 1 }]
								}
								"""))
				.andExpect(status().isForbidden());
	}

	@Test
	@WithMockUser(roles = "WAITER")
	void waiterCanAccessOrders() throws Exception {
		when(orderService.getOpenOrders()).thenReturn(List.of(sampleResponse()));

		mockMvc.perform(get("/api/waiter/orders"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].orderNumber").value("ORD-1"));
	}

	@Test
	@WithMockUser(roles = "ADMIN")
	void adminCanAccessOrders() throws Exception {
		when(orderService.getOpenOrders()).thenReturn(List.of(sampleResponse()));

		mockMvc.perform(get("/api/waiter/orders"))
				.andExpect(status().isOk());
	}

	@Test
	@WithMockUser(roles = "WAITER")
	void createReturns201() throws Exception {
		when(orderService.createOrder(any())).thenReturn(sampleResponse());

		mockMvc.perform(post("/api/waiter/orders")
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "diningTableId": 1,
								  "items": [{ "menuItemId": 10, "quantity": 2 }]
								}
								"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.status").value("ACCEPTED"))
				.andExpect(jsonPath("$.closed").value(false))
				.andExpect(jsonPath("$.totalAmount").value(25.00));
	}

	@Test
	@WithMockUser(roles = "WAITER")
	void invalidRequestReturns400() throws Exception {
		when(orderService.createOrder(any()))
				.thenThrow(new InvalidRequestException("Order must contain at least one item"));

		mockMvc.perform(post("/api/waiter/orders")
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "diningTableId": 1,
								  "items": []
								}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status").value(400));
	}

	@Test
	@WithMockUser(roles = "WAITER")
	void missingResourceReturns404() throws Exception {
		when(orderService.getOrderById(eq(99L)))
				.thenThrow(new ResourceNotFoundException("Order not found: 99"));

		mockMvc.perform(get("/api/waiter/orders/99"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.status").value(404));
	}

	@Test
	@WithMockUser(roles = "WAITER")
	void businessRuleReturns409() throws Exception {
		when(orderService.createOrder(any()))
				.thenThrow(new BusinessRuleException("Dining table is not AVAILABLE"));

		mockMvc.perform(post("/api/waiter/orders")
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "diningTableId": 1,
								  "items": [{ "menuItemId": 10, "quantity": 1 }]
								}
								"""))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.status").value(409));
	}

	private OrderResponse sampleResponse() {
		LocalDateTime now = LocalDateTime.of(2026, 8, 5, 12, 0);
		OrderItemResponse item = new OrderItemResponse(
				1L,
				10L,
				"Grilled Salmon",
				new BigDecimal("12.50"),
				2,
				new BigDecimal("25.00"));
		return new OrderResponse(
				1L,
				"ORD-1",
				1L,
				3,
				5L,
				"Waiter One",
				"ACCEPTED",
				false,
				new BigDecimal("25.00"),
				now,
				now,
				List.of(item));
	}
}
