package bg.martinandonov.restaurant.reservation.dto;

import java.time.LocalDateTime;

public class UpdateReservationRequest {

	private Long diningTableId;
	private LocalDateTime startTime;
	private LocalDateTime endTime;
	private Integer guestCount;
	private String notes;

	public Long getDiningTableId() {
		return diningTableId;
	}

	public void setDiningTableId(Long diningTableId) {
		this.diningTableId = diningTableId;
	}

	public LocalDateTime getStartTime() {
		return startTime;
	}

	public void setStartTime(LocalDateTime startTime) {
		this.startTime = startTime;
	}

	public LocalDateTime getEndTime() {
		return endTime;
	}

	public void setEndTime(LocalDateTime endTime) {
		this.endTime = endTime;
	}

	public Integer getGuestCount() {
		return guestCount;
	}

	public void setGuestCount(Integer guestCount) {
		this.guestCount = guestCount;
	}

	public String getNotes() {
		return notes;
	}

	public void setNotes(String notes) {
		this.notes = notes;
	}
}
