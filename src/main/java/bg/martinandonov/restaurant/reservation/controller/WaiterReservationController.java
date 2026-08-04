package bg.martinandonov.restaurant.reservation.controller;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import bg.martinandonov.restaurant.common.exception.InvalidRequestException;
import bg.martinandonov.restaurant.reservation.dto.ReservationScheduleEntryResponse;
import bg.martinandonov.restaurant.reservation.entity.ReservationStatus;
import bg.martinandonov.restaurant.reservation.service.ReservationService;

@RestController
@RequestMapping("/api/waiter/reservations")
public class WaiterReservationController {

	private final ReservationService reservationService;

	public WaiterReservationController(ReservationService reservationService) {
		this.reservationService = reservationService;
	}

	@GetMapping("/schedule")
	public ResponseEntity<List<ReservationScheduleEntryResponse>> schedule(
			@RequestParam LocalDateTime from,
			@RequestParam LocalDateTime to,
			@RequestParam(required = false) Long tableId,
			@RequestParam(required = false) String status) {
		return ResponseEntity.ok(reservationService.getSchedule(
				from, to, tableId, parseOptionalStatus(status), true));
	}

	private ReservationStatus parseOptionalStatus(String status) {
		if (status == null || status.isBlank()) {
			return null;
		}
		try {
			return ReservationStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
		}
		catch (IllegalArgumentException ex) {
			throw new InvalidRequestException("Unknown reservation status: " + status.trim());
		}
	}
}
