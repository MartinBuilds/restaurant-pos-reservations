package bg.martinandonov.restaurant.security;

import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Exposes the session CSRF token for authenticated clients that need it for STOMP CONNECT.
 * HTTP session authentication remains the WebSocket handshake credential.
 */
@RestController
public class CsrfTokenController {

	@GetMapping("/api/csrf")
	public CsrfToken csrf(CsrfToken token) {
		return token;
	}
}