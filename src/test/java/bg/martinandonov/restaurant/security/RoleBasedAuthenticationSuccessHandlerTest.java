package bg.martinandonov.restaurant.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

class RoleBasedAuthenticationSuccessHandlerTest {

	@Test
	void adminRedirectsToAdmin() {
		assertThat(RoleBasedAuthenticationSuccessHandler.resolveTarget(auth("ROLE_ADMIN")))
				.isEqualTo("/admin");
	}

	@Test
	void waiterRedirectsToWaiter() {
		assertThat(RoleBasedAuthenticationSuccessHandler.resolveTarget(auth("ROLE_WAITER")))
				.isEqualTo("/waiter");
	}

	@Test
	void cookRedirectsToKitchen() {
		assertThat(RoleBasedAuthenticationSuccessHandler.resolveTarget(auth("ROLE_COOK")))
				.isEqualTo("/kitchen");
	}

	@Test
	void clientRedirectsToClient() {
		assertThat(RoleBasedAuthenticationSuccessHandler.resolveTarget(auth("ROLE_CLIENT")))
				.isEqualTo("/client");
	}

	@Test
	void adminPrecedenceWinsOverOtherRoles() {
		assertThat(RoleBasedAuthenticationSuccessHandler.resolveTarget(
				auth("ROLE_CLIENT", "ROLE_COOK", "ROLE_WAITER", "ROLE_ADMIN")))
				.isEqualTo("/admin");
	}

	@Test
	void waiterPrecedenceOverCookAndClient() {
		assertThat(RoleBasedAuthenticationSuccessHandler.resolveTarget(
				auth("ROLE_CLIENT", "ROLE_COOK", "ROLE_WAITER")))
				.isEqualTo("/waiter");
	}

	@Test
	void cookPrecedenceOverClient() {
		assertThat(RoleBasedAuthenticationSuccessHandler.resolveTarget(
				auth("ROLE_CLIENT", "ROLE_COOK")))
				.isEqualTo("/kitchen");
	}

	@Test
	void unknownRoleFallsBackToRoot() {
		assertThat(RoleBasedAuthenticationSuccessHandler.resolveTarget(auth("ROLE_UNKNOWN")))
				.isEqualTo("/");
	}

	@Test
	void resolveTargetIgnoresRequestParameters_noOpenRedirectSurface() {
		// Handler uses only Authentication authorities — never request redirect params.
		assertThat(RoleBasedAuthenticationSuccessHandler.resolveTarget(auth("ROLE_CLIENT")))
				.isEqualTo("/client");
		assertThat(RoleBasedAuthenticationSuccessHandler.resolveTarget(auth("ROLE_ADMIN")))
				.isEqualTo("/admin");
	}

	private static Authentication auth(String... roles) {
		return new UsernamePasswordAuthenticationToken(
				"user",
				"n/a",
				List.of(roles).stream().map(SimpleGrantedAuthority::new).toList());
	}
}