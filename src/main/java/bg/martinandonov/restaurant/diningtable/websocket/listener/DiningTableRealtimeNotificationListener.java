package bg.martinandonov.restaurant.diningtable.websocket.listener;

import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import bg.martinandonov.restaurant.diningtable.websocket.dto.DiningTableRealtimeMessage;
import bg.martinandonov.restaurant.diningtable.websocket.event.DiningTableStatusChangedRealtimeEvent;
import bg.martinandonov.restaurant.kitchen.websocket.config.KitchenWebSocketConfig;

@Component
public class DiningTableRealtimeNotificationListener {

	private static final Logger log = LoggerFactory.getLogger(DiningTableRealtimeNotificationListener.class);

	private final SimpMessagingTemplate messagingTemplate;

	public DiningTableRealtimeNotificationListener(SimpMessagingTemplate messagingTemplate) {
		this.messagingTemplate = messagingTemplate;
	}

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = false)
	public void onTableStatusChanged(DiningTableStatusChangedRealtimeEvent event) {
		DiningTableRealtimeMessage message =
				DiningTableRealtimeMessage.statusChanged(event.getTableId(), event.getStatus());
		try {
			messagingTemplate.convertAndSend(KitchenWebSocketConfig.WAITER_TABLES_TOPIC, message);
		}
		catch (MessagingException ex) {
			log.warn(
					"Failed to deliver table realtime notification tableId={} status={}: {}",
					event.getTableId(),
					event.getStatus(),
					ex.getMessage());
		}
	}
}
