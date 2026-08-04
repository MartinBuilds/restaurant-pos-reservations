package bg.martinandonov.restaurant.reservation.dto;

import java.time.LocalDateTime;

public class ReservationScheduleEntryResponse {

	private final Long reservationId;
	private final String reservationNumber;
	private final Long diningTableId;
	private final Integer tableNumber;
	private final Long clientId;
	private final String clientName;
	private final LocalDateTime startTime;
	private final LocalDateTime endTime;
	private final Integer guestCount;
	private final String status;

	public ReservationScheduleEntryResponse(
			Long reservationId,
			String reservationNumber,
			Long diningTableId,
			Integer tableNumber,
			Long clientId,
			String clientName,
			LocalDateTime startTime,
			LocalDateTime endTime,
			Integer guestCount,
			String status) {
		this.reservationId = reservationId;
		this.reservationNumber = reservationNumber;
		this.diningTableId = diningTableId;
		this.tableNumber = tableNumber;
		this.clientId = clientId;
		this.clientName = clientName;
		this.startTime = startTime;
		this.endTime = endTime;
		this.guestCount = guestCount;
		this.status = status;
	}

	public Long getReservationId() {
		return reservationId;
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

	public Long getClientId() {
		return clientId;
	}

	public String getClientName() {
		return clientName;
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
}
