package bg.martinandonov.restaurant.diningtable.service;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import bg.martinandonov.restaurant.diningtable.entity.DiningTable;
import bg.martinandonov.restaurant.diningtable.entity.DiningTableStatus;
import bg.martinandonov.restaurant.diningtable.websocket.event.DiningTableStatusChangedRealtimeEvent;
import bg.martinandonov.restaurant.reservation.service.DiningTableReservationGuard;

/**
 * Keeps floor status consistent with open orders and confirmed reservations.
 * Never overrides OUT_OF_SERVICE / inactive tables.
 */
@Service
public class DiningTableFloorStatusService {

	private final DiningTableOperationalGuard diningTableOperationalGuard;
	private final DiningTableReservationGuard diningTableReservationGuard;
	private final ApplicationEventPublisher applicationEventPublisher;

	public DiningTableFloorStatusService(
			DiningTableOperationalGuard diningTableOperationalGuard,
			DiningTableReservationGuard diningTableReservationGuard,
			ApplicationEventPublisher applicationEventPublisher) {
		this.diningTableOperationalGuard = diningTableOperationalGuard;
		this.diningTableReservationGuard = diningTableReservationGuard;
		this.applicationEventPublisher = applicationEventPublisher;
	}

	public DiningTableStatus resolveEffectiveStatus(DiningTable table) {
		if (table == null) {
			return DiningTableStatus.AVAILABLE;
		}
		if (!table.isActive() || table.getStatus() == DiningTableStatus.OUT_OF_SERVICE) {
			return DiningTableStatus.OUT_OF_SERVICE;
		}
		if (diningTableOperationalGuard.hasOpenOrder(table.getId())) {
			return DiningTableStatus.OCCUPIED;
		}
		if (diningTableReservationGuard.hasActiveOrUpcomingConfirmedReservation(table.getId())) {
			return DiningTableStatus.RESERVED;
		}
		return DiningTableStatus.AVAILABLE;
	}

	/**
	 * Updates stored status from operational truth and notifies waiters after commit.
	 *
	 * @return true when stored status changed
	 */
	public boolean syncStoredStatus(DiningTable table, boolean notify) {
		if (table == null || !table.isActive() || table.getStatus() == DiningTableStatus.OUT_OF_SERVICE) {
			return false;
		}
		DiningTableStatus previous = table.getStatus();
		DiningTableStatus next = resolveEffectiveStatus(table);
		if (previous == next) {
			if (notify) {
				applicationEventPublisher.publishEvent(
						new DiningTableStatusChangedRealtimeEvent(table.getId(), next.name()));
			}
			return false;
		}
		table.setStatus(next);
		if (notify) {
			applicationEventPublisher.publishEvent(
					new DiningTableStatusChangedRealtimeEvent(table.getId(), next.name()));
		}
		return true;
	}
}
