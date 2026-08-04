package bg.martinandonov.restaurant.reservation.dto;

import java.time.LocalDateTime;

public class ReservationResponse {

	private final Long id;
	private final String reservationNumber;
	private final Long diningTableId;
	private final Integer tableNumber;
	private final String tableDisplayName;
	private final Long clientId;
	private final String clientName;
	private final String clientEmail;
	private final LocalDateTime startTime;
	private final LocalDateTime endTime;
	private final Integer guestCount;
	private final String status;
	private final String notes;
	private final LocalDateTime createdAt;
	private final LocalDateTime updatedAt;

	public ReservationResponse(
			Long id,
			String reservationNumber,
			Long diningTableId,
			Integer tableNumber,
			String tableDisplayName,
			Long clientId,
			String clientName,
			String clientEmail,
			LocalDateTime startTime,
			LocalDateTime endTime,
			Integer guestCount,
			String status,
			String notes,
			LocalDateTime createdAt,
			LocalDateTime updatedAt) {
		this.id = id;
		this.reservationNumber = reservationNumber;
		this.diningTableId = diningTableId;
		this.tableNumber = tableNumber;
		this.tableDisplayName = tableDisplayName;
		this.clientId = clientId;
		this.clientName = clientName;
		this.clientEmail = clientEmail;
		this.startTime = startTime;
		this.endTime = endTime;
		this.guestCount = guestCount;
		this.status = status;
		this.notes = notes;
		this.createdAt = createdAt;
		this.updatedAt = updatedAt;
	}

	public Long getId() {
		return id;
	}

	public String getReservationNumber() {
		return reservationNumber;
	}

	public Long getDiningTableId() {
		return diningTableId;
	}

	public Integer getTableNumber() {
		return tableNumber;
	}

	public String getTableDisplayName() {
		return tableDisplayName;
	}

	public Long getClientId() {
		return clientId;
	}

	public String getClientName() {
		return clientName;
	}

	public String getClientEmail() {
		return clientEmail;
	}

	public LocalDateTime getStartTime() {
		return startTime;
	}

	public LocalDateTime getEndTime() {
		return endTime;
	}

	public Integer getGuestCount() {
		return guestCount;
	}

	public String getStatus() {
		return status;
	}

	public String getNotes() {
		return notes;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public LocalDateTime getUpdatedAt() {
		return updatedAt;
	}
}
