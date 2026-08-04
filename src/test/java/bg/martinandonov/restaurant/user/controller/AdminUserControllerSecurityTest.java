package bg.martinandonov.restaurant.user.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

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

import bg.martinandonov.restaurant.common.PublicController;
import bg.martinandonov.restaurant.common.exception.GlobalExceptionHandler;
import bg.martinandonov.restaurant.security.SecurityConfig;
import bg.martinandonov.restaurant.user.dto.UserResponse;
import bg.martinandonov.restaurant.user.service.UserService;

@WebMvcTest(controllers = { AdminUserController.class, PublicController.class })
@Import({ SecurityConfig.class, GlobalExceptionHandler.class })
class AdminUserControllerSecurityTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private UserService userService;

	@MockitoBean
	private UserDetailsService userDetailsService;

	@Test
	@WithAnonymousUser
	void anonymousCannotAccessAdminUsers() throws Exception {
		mockMvc.perform(get("/api/admin/users"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	@WithMockUser(roles = "WAITER")
	void waiterCannotAccessAdminUsers() throws Exception {
		mockMvc.perform(get("/api/admin/users"))
				.andExpect(status().isForbidden());
	}

	@Test
	@WithMockUser(roles = "ADMIN")
	void adminCanAccessAdminUsers() throws Exception {
		when(userService.getAllUsers()).thenReturn(List.of(
				new UserResponse(1L, "admin@example.com", "Admin", true, Set.of("ADMIN"))));

		mockMvc.perform(get("/api/admin/users"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].email").value("admin@example.com"))
				.andExpect(jsonPath("$[0].password").doesNotExist());
	}

	@Test
	@WithMockUser(roles = "ADMIN")
	void adminCanCreateUser() throws Exception {
		when(userService.createUser(any())).thenReturn(
				new UserResponse(2L, "waiter@example.com", "Waiter", true, new LinkedHashSet<>(Set.of("WAITER"))));

		mockMvc.perform(post("/api/admin/users")
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "email": "waiter@example.com",
								  "password": "password123",
								  "fullName": "Waiter",
								  "roles": ["WAITER"]
								}
								"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.email").value("waiter@example.com"))
				.andExpect(jsonPath("$.password").doesNotExist());
	}

	@Test
	@WithAnonymousUser
	void publicHealthIsAccessibleWithoutAuthentication() throws Exception {
		mockMvc.perform(get("/api/public/health"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("UP"));
	}
}
