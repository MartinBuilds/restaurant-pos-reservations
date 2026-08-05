package bg.martinandonov.restaurant.report.dto;

import java.time.LocalDateTime;
import java.util.Objects;

public final class ReportPeriodResponse {

	private final LocalDateTime from;
	private final LocalDateTime to;
	private final String timeZone;

	public ReportPeriodResponse(LocalDateTime from, LocalDateTime to, String timeZone) {
		this.from = Objects.requireNonNull(from, "from must not be null");
		this.to = Objects.requireNonNull(to, "to must not be null");
		this.timeZone = Objects.requireNonNull(timeZone, "timeZone must not be null");
	}

	public LocalDateTime getFrom() {
		return from;
	}

	public LocalDateTime getTo() {
		return to;
	}

	public String getTimeZone() {
		return timeZone;
	}
}