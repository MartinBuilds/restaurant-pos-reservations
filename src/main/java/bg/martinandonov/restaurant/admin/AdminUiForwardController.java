package bg.martinandonov.restaurant.admin;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Forwards /admin and /admin/ to the static admin SPA entry point.
 * No template engine is used.
 */
@Controller
public class AdminUiForwardController {

	@GetMapping({ "/admin", "/admin/" })
	public String adminIndex() {
		return "forward:/admin/index.html";
	}
}