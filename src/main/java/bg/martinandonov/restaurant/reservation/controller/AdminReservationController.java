package bg.martinandonov.restaurant.reservation.controller;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import bg.martinandonov.restaurant.common.exception.InvalidRequestException;
import bg.martinandonov.restaurant.reservation.dto.CreateAdminReservationRequest;
import bg.martinandonov.restaurant.reservation.dto.ReservationResponse;
import bg.martinandonov.restaurant.reservation.dto.ReservationScheduleEntryResponse;
import bg.martinandonov.restaurant.reservation.dto.UpdateReservationRequest;
import bg.martinandonov.restaurant.reservation.dto.UpdateReservationStatusRequest;
import bg.martinandonov.restaurant.reservation.entity.ReservationStatus;
import bg.martinandonov.restaurant.reservation.service.ReservationService;

@RestController
@RequestMapping("/api/admin/reservations")
public class AdminReservationController {

	private final ReservationService reservationService;

	public AdminReservationController(ReservationService reservationService) {
		this.reservationService = reservationService;
	}

	@PostMapping
	public ResponseEntity<ReservationResponse> create(@RequestBody CreateAdminReservationRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(reservationService.createByAdmin(request));
	}

	@GetMapping
	public ResponseEntity<List<ReservationResponse>> list(
			@RequestParam(required = false) LocalDateTime from,
			@RequestParam(required = false) LocalDateTime to,
			@RequestParam(required = false) String status,
			@RequestParam(required = false) Long tableId,
			@RequestParam(required = false) Long clientId) {
		return ResponseEntity.ok(reservationService.getReservationsForAdmin(
				from, to, parseOptionalStatus(status), tableId, clientId));
	}

	@GetMapping("/schedule")
	public ResponseEntity<List<ReservationScheduleEntryResponse>> schedule(
			@RequestParam LocalDateTime from,
			@RequestParam LocalDateTime to,
			@RequestParam(required = false) Long tableId,
			@RequestParam(required = false) String status) {
		return ResponseEntity.ok(reservationService.getSchedule(
				from, to, tableId, parseOptionalStatus(status), false));
	}

	@GetMapping("/{id}")
	public ResponseEntity<ReservationResponse> getById(@PathVariable Long id) {
		return ResponseEntity.ok(reservationService.getReservationByIdForAdmin(id));
	}

	@PutMapping("/{id}")
	public ResponseEntity<ReservationResponse> update(
			@PathVariable Long id,
			@RequestBody UpdateReservationRequest request) {
		return ResponseEntity.ok(reservationService.updateReservationByAdmin(id, request));
	}

	@PatchMapping("/{id}/status")
	public ResponseEntity<ReservationResponse> updateStatus(
			@PathVariable Long id,
			@RequestBody UpdateReservationStatusRequest request) {
		return ResponseEntity.ok(reservationService.updateReservationStatusByAdmin(id, request));
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
