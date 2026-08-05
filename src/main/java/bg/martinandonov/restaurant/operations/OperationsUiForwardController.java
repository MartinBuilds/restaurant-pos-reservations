package bg.martinandonov.restaurant.operations;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Forwards waiter/kitchen entry URLs to static SPA index pages.
 * No template engine is used.
 */
@Controller
public class OperationsUiForwardController {

	@GetMapping({ "/waiter", "/waiter/" })
	public String waiterIndex() {
		return "forward:/waiter/index.html";
	}

	@GetMapping({ "/kitchen", "/kitchen/" })
	public String kitchenIndex() {
		return "forward:/kitchen/index.html";
	}
}