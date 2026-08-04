package bg.martinandonov.restaurant.kitchen.websocket.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class KitchenWebSocketConfig implements WebSocketMessageBrokerConfigurer {

	public static final String ENDPOINT = "/ws";
	public static final String TOPIC_PREFIX = "/topic";
	public static final String APPLICATION_PREFIX = "/app";
	public static final String KITCHEN_ORDERS_TOPIC = "/topic/kitchen/orders";
	public static final String WAITER_ORDERS_TOPIC = "/topic/waiter/orders";
	public static final String HEARTBEAT_SCHEDULER_BEAN = "kitchenWebSocketHeartbeatScheduler";

	private static final long HEARTBEAT_MS = 10_000L;

	@Bean(name = HEARTBEAT_SCHEDULER_BEAN, destroyMethod = "shutdown")
	public ThreadPoolTaskScheduler kitchenWebSocketHeartbeatScheduler() {
		ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
		scheduler.setPoolSize(1);
		scheduler.setThreadNamePrefix("kitchen-ws-heartbeat-");
		scheduler.setWaitForTasksToCompleteOnShutdown(true);
		scheduler.setAwaitTerminationSeconds(5);
		scheduler.initialize();
		return scheduler;
	}

	@Override
	public void configureMessageBroker(MessageBrokerRegistry registry) {
		registry.setApplicationDestinationPrefixes(APPLICATION_PREFIX);
		registry.enableSimpleBroker(TOPIC_PREFIX)
				.setHeartbeatValue(new long[] { HEARTBEAT_MS, HEARTBEAT_MS })
				.setTaskScheduler(kitchenWebSocketHeartbeatScheduler());
	}

	@Override
	public void registerStompEndpoints(StompEndpointRegistry registry) {
		registry.addEndpoint(ENDPOINT)
				.setAllowedOriginPatterns(
						"http://localhost:*",
						"https://localhost:*",
						"http://127.0.0.1:*",
						"https://127.0.0.1:*");
	}
}
