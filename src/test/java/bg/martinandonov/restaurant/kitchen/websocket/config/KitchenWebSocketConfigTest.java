package bg.martinandonov.restaurant.kitchen.websocket.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.config.SimpleBrokerRegistration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.StompWebSocketEndpointRegistration;

class KitchenWebSocketConfigTest {

	@Test
	void registersWsEndpointWithoutSockJs() {
		KitchenWebSocketConfig config = new KitchenWebSocketConfig();
		StompEndpointRegistry registry = mock(StompEndpointRegistry.class);
		StompWebSocketEndpointRegistration registration = mock(StompWebSocketEndpointRegistration.class);
		when(registry.addEndpoint("/ws")).thenReturn(registration);
		when(registration.setAllowedOriginPatterns(any(String[].class))).thenReturn(registration);

		config.registerStompEndpoints(registry);

		verify(registry).addEndpoint("/ws");
		verify(registration, never()).withSockJS();
		verify(registration).setAllowedOriginPatterns(
				"http://localhost:*",
				"https://localhost:*",
				"http://127.0.0.1:*",
				"https://127.0.0.1:*");
	}

	@Test
	void configuresSimpleBrokerHeartbeatsAndAppPrefix() {
		KitchenWebSocketConfig config = new KitchenWebSocketConfig();
		MessageBrokerRegistry registry = mock(MessageBrokerRegistry.class);
		SimpleBrokerRegistration brokerRegistration = mock(SimpleBrokerRegistration.class);
		when(registry.enableSimpleBroker("/topic")).thenReturn(brokerRegistration);
		when(brokerRegistration.setHeartbeatValue(any(long[].class))).thenReturn(brokerRegistration);
		when(brokerRegistration.setTaskScheduler(any())).thenReturn(brokerRegistration);

		config.configureMessageBroker(registry);

		verify(registry).setApplicationDestinationPrefixes("/app");
		verify(registry).enableSimpleBroker("/topic");
		verify(brokerRegistration).setHeartbeatValue(new long[] { 10_000L, 10_000L });
		verify(brokerRegistration).setTaskScheduler(any(ThreadPoolTaskScheduler.class));
	}

	@Test
	void heartbeatSchedulerBeanUsesSmallPool() {
		KitchenWebSocketConfig config = new KitchenWebSocketConfig();
		ThreadPoolTaskScheduler scheduler = config.kitchenWebSocketHeartbeatScheduler();
		try {
			assertThat(scheduler).isNotNull();
			assertThat(scheduler.getThreadNamePrefix()).isEqualTo("kitchen-ws-heartbeat-");
			Object configuredPoolSize = org.springframework.test.util.ReflectionTestUtils.getField(scheduler, "poolSize");
			assertThat(configuredPoolSize).isEqualTo(1);
		}
		finally {
			scheduler.shutdown();
		}
	}

	@Test
	void noMessageMappingBusinessControllersExist() {
		ClassPathScanningCandidateComponentProvider scanner =
				new ClassPathScanningCandidateComponentProvider(false);
		scanner.addIncludeFilter(new AnnotationTypeFilter(MessageMapping.class));
		assertThat(scanner.findCandidateComponents("bg.martinandonov.restaurant")).isEmpty();
	}

	@Test
	void topicConstantsMatchContract() {
		assertThat(KitchenWebSocketConfig.ENDPOINT).isEqualTo("/ws");
		assertThat(KitchenWebSocketConfig.TOPIC_PREFIX).isEqualTo("/topic");
		assertThat(KitchenWebSocketConfig.APPLICATION_PREFIX).isEqualTo("/app");
		assertThat(KitchenWebSocketConfig.KITCHEN_ORDERS_TOPIC).isEqualTo("/topic/kitchen/orders");
		assertThat(KitchenWebSocketConfig.WAITER_ORDERS_TOPIC).isEqualTo("/topic/waiter/orders");
	}
}