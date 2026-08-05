package bg.martinandonov.restaurant.client;

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

@WebMvcTest(controllers = ClientUiForwardController.class)
@Import(SecurityConfig.class)
class ClientUiSecurityTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private UserDetailsService userDetailsService;

	@Test
	@WithAnonymousUser
	void anonymousClientDeniedOrRedirectedToLogin() throws Exception {
		MvcResult result = mockMvc.perform(get("/client")).andReturn();
		org.assertj.core.api.Assertions.assertThat(result.getResponse().getStatus()).isIn(302, 401);
	}

	@Test
	@WithMockUser(roles = "ADMIN")
	void adminWithoutClientDenied() throws Exception {
		mockMvc.perform(get("/client")).andExpect(status().isForbidden());
		mockMvc.perform(get("/client/")).andExpect(status().isForbidden());
	}

	@Test
	@WithMockUser(roles = "WAITER")
	void waiterDenied() throws Exception {
		mockMvc.perform(get("/client")).andExpect(status().isForbidden());
	}

	@Test
	@WithMockUser(roles = "COOK")
	void cookDenied() throws Exception {
		mockMvc.perform(get("/client/")).andExpect(status().isForbidden());
	}

	@Test
	@WithMockUser(roles = "CLIENT")
	void clientCanOpenClient() throws Exception {
		mockMvc.perform(get("/client"))
				.andExpect(status().isOk())
				.andExpect(forwardedUrl("/client/index.html"));
	}

	@Test
	@WithMockUser(roles = "CLIENT")
	void clientCanOpenClientSlash() throws Exception {
		mockMvc.perform(get("/client/"))
				.andExpect(status().isOk())
				.andExpect(forwardedUrl("/client/index.html"));
	}
}