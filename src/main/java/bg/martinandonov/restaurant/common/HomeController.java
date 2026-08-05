package bg.martinandonov.restaurant.common;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

	@GetMapping("/")
	public String home(Authentication authentication) {
		if (isAnonymous(authentication)) {
			return "redirect:/login";
		}
		if (hasRole(authentication, "ROLE_ADMIN")) {
			return "redirect:/admin";
		}
		if (hasRole(authentication, "ROLE_WAITER")) {
			return "redirect:/waiter";
		}
		if (hasRole(authentication, "ROLE_COOK")) {
			return "redirect:/kitchen";
		}
		return "redirect:/login";
	}

	private static boolean isAnonymous(Authentication authentication) {
		return authentication == null
				|| !authentication.isAuthenticated()
				|| authentication instanceof AnonymousAuthenticationToken;
	}

	private static boolean hasRole(Authentication authentication, String role) {
		for (GrantedAuthority authority : authentication.getAuthorities()) {
			if (role.equals(authority.getAuthority())) {
				return true;
			}
		}
		return false;
	}
}