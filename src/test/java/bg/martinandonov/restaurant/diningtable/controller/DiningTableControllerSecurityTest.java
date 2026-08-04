package bg.martinandonov.restaurant.diningtable.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import bg.martinandonov.restaurant.diningtable.dto.DiningTableResponse;
import bg.martinandonov.restaurant.diningtable.service.DiningTableService;
import bg.martinandonov.restaurant.security.SecurityConfig;

@WebMvcTest(controllers = { AdminDiningTableController.class, WaiterDiningTableController.class })
@Import({ SecurityConfig.class, GlobalExceptionHandler.class })
class DiningTableControllerSecurityTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private DiningTableService diningTableService;

	@MockitoBean
	private UserDetailsService userDetailsService;

	@Test
	@WithAnonymousUser
	void anonymousCannotAccessAdminTables() throws Exception {
		mockMvc.perform(get("/api/admin/tables"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	@WithAnonymousUser
	void anonymousCannotAccessWaiterTables() throws Exception {
		mockMvc.perform(get("/api/waiter/tables"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	@WithMockUser(roles = "WAITER")
	void waiterCannotAccessAdminTables() throws Exception {
		mockMvc.perform(get("/api/admin/tables"))
				.andExpect(status().isForbidden());
	}

	@Test
	@WithMockUser(roles = "WAITER")
	void waiterCanAccessWaiterTables() throws Exception {
		when(diningTableService.getActiveTables()).thenReturn(List.of(sampleResponse()));

		mockMvc.perform(get("/api/waiter/tables"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].tableNumber").value(5));
	}

	@Test
	@WithMockUser(roles = "ADMIN")
	void adminCanAccessAdminTables() throws Exception {
		when(diningTableService.getAllTables()).thenReturn(List.of(sampleResponse()));

		mockMvc.perform(get("/api/admin/tables"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].status").value("AVAILABLE"));
	}

	@Test
	@WithMockUser(roles = "ADMIN")
	void adminCanAccessWaiterTables() throws Exception {
		when(diningTableService.getActiveTables()).thenReturn(List.of(sampleResponse()));

		mockMvc.perform(get("/api/waiter/tables"))
				.andExpect(status().isOk());
	}

	@Test
	@WithMockUser(roles = "COOK")
	void cookCannotAccessAdminOrWaiterTables() throws Exception {
		mockMvc.perform(get("/api/admin/tables"))
				.andExpect(status().isForbidden());
		mockMvc.perform(get("/api/waiter/tables"))
				.andExpect(status().isForbidden());
	}

	@Test
	@WithMockUser(roles = "CLIENT")
	void clientCannotAccessAdminOrWaiterTables() throws Exception {
		mockMvc.perform(get("/api/admin/tables"))
				.andExpect(status().isForbidden());
		mockMvc.perform(get("/api/waiter/tables"))
				.andExpect(status().isForbidden());
	}

	@Test
	@WithMockUser(roles = "ADMIN")
	void createReturns201() throws Exception {
		when(diningTableService.createTable(any())).thenReturn(sampleResponse());

		mockMvc.perform(post("/api/admin/tables")
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "tableNumber": 5,
								  "displayName": "SMOKE Window",
								  "capacity": 4
								}
								"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.tableNumber").value(5));
	}

	@Test
	@WithMockUser(roles = "ADMIN")
	void invalidRequestReturns400() throws Exception {
		when(diningTableService.createTable(any()))
				.thenThrow(new InvalidRequestException("capacity must be between 1 and 50"));

		mockMvc.perform(post("/api/admin/tables")
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "tableNumber": 5,
								  "capacity": 99
								}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status").value(400));
	}

	@Test
	@WithMockUser(roles = "ADMIN")
	void missingResourceReturns404() throws Exception {
		when(diningTableService.getTableById(eq(99L)))
				.thenThrow(new ResourceNotFoundException("Dining table not found: 99"));

		mockMvc.perform(get("/api/admin/tables/99"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.status").value(404));
	}

	@Test
	@WithMockUser(roles = "ADMIN")
	void duplicateNumberReturns409() throws Exception {
		when(diningTableService.createTable(any()))
				.thenThrow(new BusinessRuleException("A dining table with this number already exists"));

		mockMvc.perform(post("/api/admin/tables")
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "tableNumber": 5,
								  "capacity": 4
								}
								"""))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.status").value(409));
	}

	@Test
	@WithMockUser(roles = "WAITER")
	void invalidStatusTransitionReturns409() throws Exception {
		when(diningTableService.updateActiveTableStatus(eq(1L), any()))
				.thenThrow(new BusinessRuleException("Waiters cannot set a table to OUT_OF_SERVICE"));

		mockMvc.perform(patch("/api/waiter/tables/1/status")
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "status": "OUT_OF_SERVICE"
								}
								"""))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.status").value(409));
	}

	@Test
	@WithMockUser(roles = "ADMIN")
	void optimisticLockConflictReturns409() throws Exception {
		when(diningTableService.updateTableStatus(eq(1L), any()))
				.thenThrow(new org.springframework.orm.ObjectOptimisticLockingFailureException(
						"DiningTable", 1L));

		mockMvc.perform(patch("/api/admin/tables/1/status")
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "status": "OCCUPIED"
								}
								"""))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.status").value(409));
	}

	private DiningTableResponse sampleResponse() {
		return new DiningTableResponse(1L, 5, "Window", 4, "AVAILABLE", true, 0L);
	}
}
