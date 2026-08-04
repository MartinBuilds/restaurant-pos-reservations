package bg.martinandonov.restaurant.kitchen.websocket.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.authorization.AuthorizationResult;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.messaging.access.intercept.MessageMatcherDelegatingAuthorizationManager;

class KitchenWebSocketMessageAuthorizationTest {

	private AuthorizationManager<Message<?>> authorizationManager;

	@BeforeEach
	void setUp() {
		authorizationManager = new KitchenWebSocketSecurityConfig()
				.messageAuthorizationManager(MessageMatcherDelegatingAuthorizationManager.builder());
	}

	@AfterEach
	void tearDown() {
		SecurityContextHolder.clearContext();
	}

	@Test
	void anonymousCannotSubscribeKitchenOrWaiter() {
		Authentication anonymous = new AnonymousAuthenticationToken(
				"key", "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS"));
		assertDenied(anonymous, subscribe("/topic/kitchen/orders"));
		assertDenied(anonymous, subscribe("/topic/waiter/orders"));
	}

	@Test
	void cookCanSubscribeKitchenButNotWaiter() {
		Authentication cook = user("cook@example.com", "ROLE_COOK");
		assertGranted(cook, subscribe("/topic/kitchen/orders"));
		assertDenied(cook, subscribe("/topic/waiter/orders"));
	}

	@Test
	void waiterCanSubscribeWaiterButNotKitchen() {
		Authentication waiter = user("waiter@example.com", "ROLE_WAITER");
		assertGranted(waiter, subscribe("/topic/waiter/orders"));
		assertDenied(waiter, subscribe("/topic/kitchen/orders"));
	}

	@Test
	void adminCanSubscribeBoth() {
		Authentication admin = user("admin@example.com", "ROLE_ADMIN");
		assertGranted(admin, subscribe("/topic/kitchen/orders"));
		assertGranted(admin, subscribe("/topic/waiter/orders"));
	}

	@Test
	void clientCannotSubscribeOperationalTopics() {
		Authentication client = user("client@example.com", "ROLE_CLIENT");
		assertDenied(client, subscribe("/topic/kitchen/orders"));
		assertDenied(client, subscribe("/topic/waiter/orders"));
	}

	@Test
	void sendToAppIsDenied() {
		Authentication admin = user("admin@example.com", "ROLE_ADMIN");
		assertDenied(admin, message("/app/orders"));
	}

	@Test
	void unknownTopicIsDenied() {
		Authentication admin = user("admin@example.com", "ROLE_ADMIN");
		assertDenied(admin, subscribe("/topic/unknown/orders"));
	}

	@Test
	void connectRequiresAuthenticatedPrincipal() {
		Authentication cook = user("cook@example.com", "ROLE_COOK");
		assertGranted(cook, connect());

		Authentication anonymous = new AnonymousAuthenticationToken(
				"key", "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS"));
		assertDenied(anonymous, connect());
	}

	private void assertGranted(Authentication authentication, Message<byte[]> message) {
		AuthorizationResult result = authorizationManager.authorize(() -> authentication, message);
		assertThat(result).isInstanceOf(AuthorizationDecision.class);
		assertThat(((AuthorizationDecision) result).isGranted()).isTrue();
	}

	private void assertDenied(Authentication authentication, Message<byte[]> message) {
		AuthorizationResult result = authorizationManager.authorize(() -> authentication, message);
		assertThat(result == null || !((AuthorizationDecision) result).isGranted()).isTrue();
	}

	private static Authentication user(String email, String... roles) {
		return new UsernamePasswordAuthenticationToken(
				email, "n/a", AuthorityUtils.createAuthorityList(roles));
	}

	private static Message<byte[]> subscribe(String destination) {
		return MessageBuilder.withPayload(new byte[0])
				.setHeader(SimpMessageHeaderAccessor.MESSAGE_TYPE_HEADER, SimpMessageType.SUBSCRIBE)
				.setHeader(SimpMessageHeaderAccessor.DESTINATION_HEADER, destination)
				.build();
	}

	private static Message<byte[]> message(String destination) {
		return MessageBuilder.withPayload(new byte[0])
				.setHeader(SimpMessageHeaderAccessor.MESSAGE_TYPE_HEADER, SimpMessageType.MESSAGE)
				.setHeader(SimpMessageHeaderAccessor.DESTINATION_HEADER, destination)
				.build();
	}

	private static Message<byte[]> connect() {
		return MessageBuilder.withPayload(new byte[0])
				.setHeader(SimpMessageHeaderAccessor.MESSAGE_TYPE_HEADER, SimpMessageType.CONNECT)
				.build();
	}
}