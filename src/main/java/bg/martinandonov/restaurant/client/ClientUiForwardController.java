package bg.martinandonov.restaurant.client;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Forwards /client entry URLs to the static client reservations SPA.
 */
@Controller
public class ClientUiForwardController {

	@GetMapping({ "/client", "/client/" })
	public String clientIndex() {
		return "forward:/client/index.html";
	}
}