package bg.martinandonov.restaurant.kitchen.websocket.security;

import static org.springframework.messaging.simp.SimpMessageType.MESSAGE;
import static org.springframework.messaging.simp.SimpMessageType.SUBSCRIBE;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.config.annotation.web.socket.EnableWebSocketSecurity;
import org.springframework.security.messaging.access.intercept.MessageMatcherDelegatingAuthorizationManager;

/**
 * STOMP message-level authorization.
 *
 * <p>{@link EnableWebSocketSecurity} also requires a valid CSRF token on STOMP
 * {@code CONNECT}. Clients obtain the token from {@code GET /api/csrf} (authenticated
 * session) and send it in the STOMP CONNECT headers. HTTP CSRF for REST remains unchanged.
 */
@Configuration
@EnableWebSocketSecurity
public class KitchenWebSocketSecurityConfig {

	@Bean
	AuthorizationManager<Message<?>> messageAuthorizationManager(
			MessageMatcherDelegatingAuthorizationManager.Builder messages) {
		messages
				.nullDestMatcher().authenticated()
				.simpSubscribeDestMatchers("/topic/kitchen/**").hasAnyRole("COOK", "ADMIN")
				.simpSubscribeDestMatchers("/topic/waiter/**").hasAnyRole("WAITER", "ADMIN")
				.simpDestMatchers("/app/**").denyAll()
				.simpTypeMatchers(MESSAGE, SUBSCRIBE).denyAll()
				.anyMessage().denyAll();
		return messages.build();
	}
}