package bg.martinandonov.restaurant.security;

import java.io.IOException;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Redirects after form login by role. Always uses the computed target (no saved request).
 * Precedence: ADMIN, WAITER, COOK, CLIENT.
 */
public class RoleBasedAuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

	public RoleBasedAuthenticationSuccessHandler() {
		setAlwaysUseDefaultTargetUrl(true);
		setDefaultTargetUrl("/");
	}

	@Override
	public void onAuthenticationSuccess(
			HttpServletRequest request,
			HttpServletResponse response,
			Authentication authentication) throws IOException, ServletException {
		setDefaultTargetUrl(resolveTarget(authentication));
		super.onAuthenticationSuccess(request, response, authentication);
	}

	static String resolveTarget(Authentication authentication) {
		if (hasRole(authentication, "ROLE_ADMIN")) {
			return "/admin";
		}
		if (hasRole(authentication, "ROLE_WAITER")) {
			return "/waiter";
		}
		if (hasRole(authentication, "ROLE_COOK")) {
			return "/kitchen";
		}
		if (hasRole(authentication, "ROLE_CLIENT")) {
			return "/client";
		}
		return "/";
	}

	private static boolean hasRole(Authentication authentication, String role) {
		if (authentication == null) {
			return false;
		}
		for (GrantedAuthority authority : authentication.getAuthorities()) {
			if (role.equals(authority.getAuthority())) {
				return true;
			}
		}
		return false;
	}
}