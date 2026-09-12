package bg.martinandonov.restaurant.diningtable.websocket.event;

import java.util.Objects;

public class DiningTableStatusChangedRealtimeEvent {

	private final Long tableId;
	private final String status;

	public DiningTableStatusChangedRealtimeEvent(Long tableId, String status) {
		this.tableId = Objects.requireNonNull(tableId, "tableId must not be null");
		this.status = Objects.requireNonNull(status, "status must not be null");
	}

	public Long getTableId() {
		return tableId;
	}

	public String getStatus() {
		return status;
	}
}
