package bg.martinandonov.restaurant.diningtable.websocket.dto;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class DiningTableRealtimeMessage {

	private final UUID eventId;
	private final String eventType;
	private final Instant occurredAt;
	private final Long tableId;
	private final String status;

	public DiningTableRealtimeMessage(
			UUID eventId,
			String eventType,
			Instant occurredAt,
			Long tableId,
			String status) {
		this.eventId = Objects.requireNonNull(eventId, "eventId must not be null");
		this.eventType = Objects.requireNonNull(eventType, "eventType must not be null");
		this.occurredAt = Objects.requireNonNull(occurredAt, "occurredAt must not be null");
		this.tableId = Objects.requireNonNull(tableId, "tableId must not be null");
		this.status = Objects.requireNonNull(status, "status must not be null");
	}

	public static DiningTableRealtimeMessage statusChanged(Long tableId, String status) {
		return new DiningTableRealtimeMessage(
				UUID.randomUUID(),
				"TABLE_STATUS_CHANGED",
				Instant.now(),
				tableId,
				status);
	}

	public UUID getEventId() {
		return eventId;
	}

	public String getEventType() {
		return eventType;
	}

	public Instant getOccurredAt() {
		return occurredAt;
	}

	public Long getTableId() {
		return tableId;
	}

	public String getStatus() {
		return status;
	}
}
