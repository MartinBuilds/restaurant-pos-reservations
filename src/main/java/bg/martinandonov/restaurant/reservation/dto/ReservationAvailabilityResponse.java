package bg.martinandonov.restaurant.reservation.dto;

import java.time.LocalDateTime;
import java.util.List;

public class ReservationAvailabilityResponse {

	private final LocalDateTime startTime;
	private final LocalDateTime endTime;
	private final Integer guestCount;
	private final List<AvailableTableResponse> availableTables;

	public ReservationAvailabilityResponse(
			LocalDateTime startTime,
			LocalDateTime endTime,
			Integer guestCount,
			List<AvailableTableResponse> availableTables) {
		this.startTime = startTime;
		this.endTime = endTime;
		this.guestCount = guestCount;
		this.availableTables = availableTables;
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

	public List<AvailableTableResponse> getAvailableTables() {
		return availableTables;
	}
}
