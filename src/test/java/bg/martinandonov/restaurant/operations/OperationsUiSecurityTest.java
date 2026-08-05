package bg.martinandonov.restaurant.operations;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import bg.martinandonov.restaurant.security.SecurityConfig;

@WebMvcTest(controllers = OperationsUiForwardController.class)
@Import(SecurityConfig.class)
class OperationsUiSecurityTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private UserDetailsService userDetailsService;

	@Test
	@WithAnonymousUser
	void anonymousWaiterDeniedOrRedirected() throws Exception {
		MvcResult result = mockMvc.perform(get("/waiter")).andReturn();
		org.assertj.core.api.Assertions.assertThat(result.getResponse().getStatus()).isIn(302, 401);
	}

	@Test
	@WithAnonymousUser
	void anonymousKitchenDeniedOrRedirected() throws Exception {
		MvcResult result = mockMvc.perform(get("/kitchen")).andReturn();
		org.assertj.core.api.Assertions.assertThat(result.getResponse().getStatus()).isIn(302, 401);
	}

	@Test
	@WithMockUser(roles = "CLIENT")
	void clientDeniedWaiterAndKitchen() throws Exception {
		mockMvc.perform(get("/waiter")).andExpect(status().isForbidden());
		mockMvc.perform(get("/kitchen")).andExpect(status().isForbidden());
	}

	@Test
	@WithMockUser(roles = "COOK")
	void cookDeniedWaiterAllowedKitchen() throws Exception {
		mockMvc.perform(get("/waiter")).andExpect(status().isForbidden());
		mockMvc.perform(get("/kitchen"))
				.andExpect(status().isOk())
				.andExpect(forwardedUrl("/kitchen/index.html"));
	}

	@Test
	@WithMockUser(roles = "WAITER")
	void waiterAllowedWaiterDeniedKitchen() throws Exception {
		mockMvc.perform(get("/waiter"))
				.andExpect(status().isOk())
				.andExpect(forwardedUrl("/waiter/index.html"));
		mockMvc.perform(get("/kitchen")).andExpect(status().isForbidden());
	}

	@Test
	@WithMockUser(roles = "ADMIN")
	void adminAllowedBoth() throws Exception {
		mockMvc.perform(get("/waiter/"))
				.andExpect(status().isOk())
				.andExpect(forwardedUrl("/waiter/index.html"));
		mockMvc.perform(get("/kitchen/"))
				.andExpect(status().isOk())
				.andExpect(forwardedUrl("/kitchen/index.html"));
	}
}