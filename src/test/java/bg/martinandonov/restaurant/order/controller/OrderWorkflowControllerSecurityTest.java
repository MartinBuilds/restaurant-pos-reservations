package bg.martinandonov.restaurant.order.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import bg.martinandonov.restaurant.common.exception.BusinessRuleException;
import bg.martinandonov.restaurant.common.exception.GlobalExceptionHandler;
import bg.martinandonov.restaurant.common.exception.InvalidRequestException;
import bg.martinandonov.restaurant.common.exception.ResourceNotFoundException;
import bg.martinandonov.restaurant.order.dto.KitchenOrderItemResponse;
import bg.martinandonov.restaurant.order.dto.KitchenOrderResponse;
import bg.martinandonov.restaurant.order.dto.OrderItemResponse;
import bg.martinandonov.restaurant.order.dto.OrderResponse;
import bg.martinandonov.restaurant.order.service.OrderService;
import bg.martinandonov.restaurant.order.service.OrderWorkflowService;
import bg.martinandonov.restaurant.security.SecurityConfig;

@WebMvcTest(controllers = { KitchenOrderController.class, WaiterOrderController.class })
@Import({ SecurityConfig.class, GlobalExceptionHandler.class })
class OrderWorkflowControllerSecurityTest {

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
	void anonymousCannotAccessKitchenOrWaiterStatus() throws Exception {
		mockMvc.perform(get("/api/kitchen/orders"))
				.andExpect(status().isUnauthorized());
		mockMvc.perform(patch("/api/waiter/orders/1/status")
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"status\":\"SERVED\"}"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	@WithMockUser(roles = "COOK")
	void cookCanAccessKitchenButNotWaiterStatus() throws Exception {
		when(orderWorkflowService.getKitchenOrders(null)).thenReturn(List.of(kitchenResponse()));

		mockMvc.perform(get("/api/kitchen/orders"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].status").value("ACCEPTED"));
		mockMvc.perform(patch("/api/waiter/orders/1/status")
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"status\":\"SERVED\"}"))
				.andExpect(status().isForbidden());
	}

	@Test
	@WithMockUser(roles = "WAITER")
	void waiterCanAccessWaiterStatusButNotKitchen() throws Exception {
		when(orderWorkflowService.markServedByWaiter(1L)).thenReturn(waiterResponse());

		mockMvc.perform(get("/api/kitchen/orders"))
				.andExpect(status().isForbidden());
		mockMvc.perform(patch("/api/waiter/orders/1/status")
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"status\":\"SERVED\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("SERVED"));
	}

	@Test
	@WithMockUser(roles = "ADMIN")
	void adminCanAccessBoth() throws Exception {
		when(orderWorkflowService.getKitchenOrders(null)).thenReturn(List.of(kitchenResponse()));
		when(orderWorkflowService.markServedByWaiter(1L)).thenReturn(waiterResponse());

		mockMvc.perform(get("/api/kitchen/orders"))
				.andExpect(status().isOk());
		mockMvc.perform(patch("/api/waiter/orders/1/status")
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"status\":\"SERVED\"}"))
				.andExpect(status().isOk());
	}

	@Test
	@WithMockUser(roles = "CLIENT")
	void clientCannotAccessOperationalEndpoints() throws Exception {
		mockMvc.perform(get("/api/kitchen/orders"))
				.andExpect(status().isForbidden());
		mockMvc.perform(patch("/api/waiter/orders/1/status")
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"status\":\"SERVED\"}"))
				.andExpect(status().isForbidden());
	}

	@Test
	@WithMockUser(roles = "COOK")
	void kitchenAcceptsCookingAndReadyRejectsServed() throws Exception {
		when(orderWorkflowService.updateFromKitchen(eq(1L), any())).thenReturn(kitchenResponse("COOKING"));

		mockMvc.perform(patch("/api/kitchen/orders/1/status")
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"status\":\"COOKING\"}"))
				.andExpect(status().isOk());

		when(orderWorkflowService.updateFromKitchen(eq(1L), any()))
				.thenThrow(new InvalidRequestException("Kitchen can only set COOKING or READY"));

		mockMvc.perform(patch("/api/kitchen/orders/1/status")
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"status\":\"SERVED\"}"))
				.andExpect(status().isBadRequest());
	}

	@Test
	@WithMockUser(roles = "WAITER")
	void waiterRejectsCookingAndReady() throws Exception {
		mockMvc.perform(patch("/api/waiter/orders/1/status")
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"status\":\"COOKING\"}"))
				.andExpect(status().isBadRequest());
		mockMvc.perform(patch("/api/waiter/orders/1/status")
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"status\":\"READY\"}"))
				.andExpect(status().isBadRequest());
	}

	@Test
	@WithMockUser(roles = "COOK")
	void invalidTransitionReturns409AndMissingReturns404() throws Exception {
		when(orderWorkflowService.updateFromKitchen(eq(1L), any()))
				.thenThrow(new BusinessRuleException("Invalid order status transition"));
		when(orderWorkflowService.getKitchenOrderById(99L))
				.thenThrow(new ResourceNotFoundException("Order not found: 99"));

		mockMvc.perform(patch("/api/kitchen/orders/1/status")
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"status\":\"READY\"}"))
				.andExpect(status().isConflict());
		mockMvc.perform(get("/api/kitchen/orders/99"))
				.andExpect(status().isNotFound());
	}

	@Test
	@WithMockUser(roles = "COOK")
	void lockingConflictReturns409() throws Exception {
		when(orderWorkflowService.updateFromKitchen(eq(1L), any()))
				.thenThrow(new ObjectOptimisticLockingFailureException("RestaurantOrder", 1L));

		mockMvc.perform(patch("/api/kitchen/orders/1/status")
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"status\":\"COOKING\"}"))
				.andExpect(status().isConflict());
	}

	private KitchenOrderResponse kitchenResponse() {
		return kitchenResponse("ACCEPTED");
	}

	private KitchenOrderResponse kitchenResponse(String status) {
		return new KitchenOrderResponse(
				1L,
				"ord-1",
				1L,
				5,
				status,
				LocalDateTime.of(2026, 8, 5, 1, 0),
				LocalDateTime.of(2026, 8, 5, 1, 0),
				List.of(new KitchenOrderItemResponse(10L, 10L, "Salad", 1)));
	}

	private OrderResponse waiterResponse() {
		return new OrderResponse(
				1L,
				"ord-1",
				1L,
				5,
				2L,
				"Ada Waiter",
				"SERVED",
				false,
				new BigDecimal("12.50"),
				LocalDateTime.of(2026, 8, 5, 1, 0),
				LocalDateTime.of(2026, 8, 5, 1, 5),
				List.of(new OrderItemResponse(
						10L, 10L, "Salad", new BigDecimal("12.50"), 1, new BigDecimal("12.50"))));
	}
}
