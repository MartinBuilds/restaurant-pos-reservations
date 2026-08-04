package bg.martinandonov.restaurant.reservation.controller;

import java.time.LocalDateTime;
import java.util.List;

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

import bg.martinandonov.restaurant.reservation.dto.CreateClientReservationRequest;
import bg.martinandonov.restaurant.reservation.dto.ReservationAvailabilityResponse;
import bg.martinandonov.restaurant.reservation.dto.ReservationResponse;
import bg.martinandonov.restaurant.reservation.dto.UpdateReservationRequest;
import bg.martinandonov.restaurant.reservation.service.ReservationService;

@RestController
@RequestMapping("/api/client/reservations")
public class ClientReservationController {

	private final ReservationService reservationService;

	public ClientReservationController(ReservationService reservationService) {
		this.reservationService = reservationService;
	}

	@GetMapping("/availability")
	public ResponseEntity<ReservationAvailabilityResponse> findAvailability(
			@RequestParam LocalDateTime startTime,
			@RequestParam LocalDateTime endTime,
			@RequestParam Integer guestCount) {
		return ResponseEntity.ok(reservationService.findAvailableTables(startTime, endTime, guestCount));
	}

	@PostMapping
	public ResponseEntity<ReservationResponse> create(@RequestBody CreateClientReservationRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(reservationService.createForCurrentClient(request));
	}

	@GetMapping
	public ResponseEntity<List<ReservationResponse>> listMine() {
		return ResponseEntity.ok(reservationService.getCurrentClientReservations());
	}

	@GetMapping("/{id}")
	public ResponseEntity<ReservationResponse> getMine(@PathVariable Long id) {
		return ResponseEntity.ok(reservationService.getCurrentClientReservationById(id));
	}

	@PutMapping("/{id}")
	public ResponseEntity<ReservationResponse> updateMine(
			@PathVariable Long id,
			@RequestBody UpdateReservationRequest request) {
		return ResponseEntity.ok(reservationService.updateCurrentClientReservation(id, request));
	}

	@PatchMapping("/{id}/cancel")
	public ResponseEntity<ReservationResponse> cancelMine(@PathVariable Long id) {
		return ResponseEntity.ok(reservationService.cancelCurrentClientReservation(id));
	}
}
