package bg.martinandonov.restaurant.account.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.LinkedHashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import bg.martinandonov.restaurant.account.dto.CurrentAccountResponse;
import bg.martinandonov.restaurant.account.service.AccountService;
import bg.martinandonov.restaurant.common.PublicController;
import bg.martinandonov.restaurant.common.exception.GlobalExceptionHandler;
import bg.martinandonov.restaurant.security.SecurityConfig;

@WebMvcTest(controllers = { AccountController.class, PublicController.class })
@Import({ SecurityConfig.class, GlobalExceptionHandler.class })
class AccountControllerSecurityTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private AccountService accountService;

	@MockitoBean
	private UserDetailsService userDetailsService;

	@Test
	@WithAnonymousUser
	void anonymousCannotAccessCurrentAccount() throws Exception {
		mockMvc.perform(get("/api/account/me"))
				.andExpect(status().isUnauthorized());
		verify(accountService, never()).getCurrentAccount(any());
	}

	@Test
	@WithMockUser(username = "admin@example.com", roles = "ADMIN")
	void adminCanAccessCurrentAccount() throws Exception {
		stubAccount(1L, "Admin User", "admin@example.com", "ADMIN");
		mockMvc.perform(get("/api/account/me"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(1))
				.andExpect(jsonPath("$.name").value("Admin User"))
				.andExpect(jsonPath("$.email").value("admin@example.com"))
				.andExpect(jsonPath("$.roles[0]").value("ADMIN"))
				.andExpect(jsonPath("$.password").doesNotExist())
				.andExpect(jsonPath("$.passwordHash").doesNotExist());
		verify(accountService).getCurrentAccount(any(Authentication.class));
	}

	@Test
	@WithMockUser(username = "waiter@example.com", roles = "WAITER")
	void waiterCanAccessCurrentAccount() throws Exception {
		stubAccount(2L, "Waiter One", "waiter@example.com", "WAITER");
		mockMvc.perform(get("/api/account/me"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.email").value("waiter@example.com"))
				.andExpect(jsonPath("$.roles[0]").value("WAITER"))
				.andExpect(jsonPath("$.password").doesNotExist());
	}

	@Test
	@WithMockUser(username = "cook@example.com", roles = "COOK")
	void cookCanAccessCurrentAccount() throws Exception {
		stubAccount(3L, "Cook One", "cook@example.com", "COOK");
		mockMvc.perform(get("/api/account/me"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.roles[0]").value("COOK"))
				.andExpect(jsonPath("$.password").doesNotExist());
	}

	@Test
	@WithMockUser(username = "client@example.com", roles = "CLIENT")
	void clientCanAccessCurrentAccount() throws Exception {
		stubAccount(4L, "Client One", "client@example.com", "CLIENT");
		mockMvc.perform(get("/api/account/me"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.roles[0]").value("CLIENT"))
				.andExpect(jsonPath("$.password").doesNotExist());
	}

	@Test
	@WithMockUser(username = "admin@example.com", roles = "ADMIN")
	void queryParametersDoNotImpersonateAnotherUser() throws Exception {
		stubAccount(1L, "Admin User", "admin@example.com", "ADMIN");
		mockMvc.perform(get("/api/account/me")
						.param("email", "other@example.com")
						.param("userId", "99"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.email").value("admin@example.com"))
				.andExpect(jsonPath("$.id").value(1));
		verify(accountService).getCurrentAccount(any(Authentication.class));
	}

	private void stubAccount(Long id, String name, String email, String role) {
		when(accountService.getCurrentAccount(any(Authentication.class)))
				.thenReturn(new CurrentAccountResponse(
						id, name, email, new LinkedHashSet<>(Set.of(role))));
	}
}
