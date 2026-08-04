package bg.martinandonov.restaurant.reservation.service;

import java.time.Clock;
import java.time.LocalDateTime;

import org.springframework.stereotype.Component;

import bg.martinandonov.restaurant.reservation.repository.ReservationRepository;

@Component
public class DiningTableReservationGuard {

	private final ReservationRepository reservationRepository;
	private final Clock clock;

	public DiningTableReservationGuard(ReservationRepository reservationRepository, Clock clock) {
		this.reservationRepository = reservationRepository;
		this.clock = clock;
	}

	public boolean hasFutureConfirmedReservation(Long diningTableId) {
		return reservationRepository.existsFutureConfirmedForTable(diningTableId, LocalDateTime.now(clock));
	}
}
