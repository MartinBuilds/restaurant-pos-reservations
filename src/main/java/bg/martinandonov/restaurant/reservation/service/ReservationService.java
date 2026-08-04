package bg.martinandonov.restaurant.reservation.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import bg.martinandonov.restaurant.common.exception.BusinessRuleException;
import bg.martinandonov.restaurant.common.exception.InvalidRequestException;
import bg.martinandonov.restaurant.common.exception.ResourceNotFoundException;
import bg.martinandonov.restaurant.diningtable.entity.DiningTable;
import bg.martinandonov.restaurant.diningtable.entity.DiningTableStatus;
import bg.martinandonov.restaurant.diningtable.repository.DiningTableRepository;
import bg.martinandonov.restaurant.reservation.dto.AvailableTableResponse;
import bg.martinandonov.restaurant.reservation.dto.CreateAdminReservationRequest;
import bg.martinandonov.restaurant.reservation.dto.CreateClientReservationRequest;
import bg.martinandonov.restaurant.reservation.dto.ReservationAvailabilityResponse;
import bg.martinandonov.restaurant.reservation.dto.ReservationResponse;
import bg.martinandonov.restaurant.reservation.dto.ReservationScheduleEntryResponse;
import bg.martinandonov.restaurant.reservation.dto.UpdateReservationRequest;
import bg.martinandonov.restaurant.reservation.dto.UpdateReservationStatusRequest;
import bg.martinandonov.restaurant.reservation.entity.Reservation;
import bg.martinandonov.restaurant.reservation.entity.ReservationStatus;
import bg.martinandonov.restaurant.reservation.repository.ReservationRepository;
import bg.martinandonov.restaurant.user.entity.AppUser;
import bg.martinandonov.restaurant.user.entity.RoleName;
import bg.martinandonov.restaurant.user.repository.AppUserRepository;

@Service
@Transactional
public class ReservationService {

	private static final int MAX_NOTES_LENGTH = 500;
	private static final Set<ReservationStatus> WAITER_DEFAULT_STATUSES =
			EnumSet.of(ReservationStatus.CONFIRMED, ReservationStatus.COMPLETED, ReservationStatus.NO_SHOW);

	private final ReservationRepository reservationRepository;
	private final DiningTableRepository diningTableRepository;
	private final AppUserRepository appUserRepository;
	private final Clock clock;

	public ReservationService(
			ReservationRepository reservationRepository,
			DiningTableRepository diningTableRepository,
			AppUserRepository appUserRepository,
			Clock clock) {
		this.reservationRepository = reservationRepository;
		this.diningTableRepository = diningTableRepository;
		this.appUserRepository = appUserRepository;
		this.clock = clock;
	}

	public ReservationResponse createForCurrentClient(CreateClientReservationRequest request) {
		Objects.requireNonNull(request, "request must not be null");
		AppUser client = requireAuthenticatedClient();
		return createReservation(
				client,
				request.getDiningTableId(),
				request.getStartTime(),
				request.getEndTime(),
				request.getGuestCount(),
				request.getNotes());
	}

	public ReservationResponse createByAdmin(CreateAdminReservationRequest request) {
		Objects.requireNonNull(request, "request must not be null");
		AppUser client = loadClientForAdmin(request.getClientId());
		return createReservation(
				client,
				request.getDiningTableId(),
				request.getStartTime(),
				request.getEndTime(),
				request.getGuestCount(),
				request.getNotes());
	}

	@Transactional(readOnly = true)
	public List<ReservationResponse> getCurrentClientReservations() {
		AppUser client = requireAuthenticatedClient();
		return reservationRepository.findByClientIdOrderByStart(client.getId()).stream()
				.map(this::toResponse)
				.toList();
	}

	@Transactional(readOnly = true)
	public ReservationResponse getCurrentClientReservationById(Long id) {
		AppUser client = requireAuthenticatedClient();
		Reservation reservation = reservationRepository.findByIdAndClientId(requireId(id), client.getId())
				.orElseThrow(() -> new ResourceNotFoundException("Reservation not found: " + id));
		return toResponse(reservation);
	}

	public ReservationResponse updateCurrentClientReservation(Long id, UpdateReservationRequest request) {
		Objects.requireNonNull(request, "request must not be null");
		AppUser client = requireAuthenticatedClient();
		Reservation reservation = reservationRepository.findByIdForUpdate(requireId(id))
				.orElseThrow(() -> new ResourceNotFoundException("Reservation not found: " + id));
		if (!reservation.getClient().getId().equals(client.getId())) {
			throw new ResourceNotFoundException("Reservation not found: " + id);
		}
		return reschedule(reservation, request);
	}

	public ReservationResponse cancelCurrentClientReservation(Long id) {
		AppUser client = requireAuthenticatedClient();
		Reservation reservation = reservationRepository.findByIdForUpdate(requireId(id))
				.orElseThrow(() -> new ResourceNotFoundException("Reservation not found: " + id));
		if (!reservation.getClient().getId().equals(client.getId())) {
			throw new ResourceNotFoundException("Reservation not found: " + id);
		}
		return cancelByClient(reservation);
	}

	@Transactional(readOnly = true)
	public ReservationResponse getReservationByIdForAdmin(Long id) {
		Reservation reservation = reservationRepository.findByIdWithDetails(requireId(id))
				.orElseThrow(() -> new ResourceNotFoundException("Reservation not found: " + id));
		return toResponse(reservation);
	}

	@Transactional(readOnly = true)
	public List<ReservationResponse> getReservationsForAdmin(
			LocalDateTime from,
			LocalDateTime to,
			ReservationStatus status,
			Long tableId,
			Long clientId) {
		LocalDateTime rangeFrom = from;
		LocalDateTime rangeTo = to;
		if (rangeFrom == null || rangeTo == null) {
			rangeFrom = LocalDateTime.now(clock).minusYears(1);
			rangeTo = LocalDateTime.now(clock).plusYears(1);
		}
		else {
			validateRange(rangeFrom, rangeTo);
		}
		Collection<ReservationStatus> statuses = status == null
				? EnumSet.allOf(ReservationStatus.class)
				: EnumSet.of(status);
		return reservationRepository.findSchedule(rangeFrom, rangeTo, tableId, clientId, statuses).stream()
				.map(this::toResponse)
				.toList();
	}

	public ReservationResponse updateReservationByAdmin(Long id, UpdateReservationRequest request) {
		Objects.requireNonNull(request, "request must not be null");
		Reservation reservation = reservationRepository.findByIdForUpdate(requireId(id))
				.orElseThrow(() -> new ResourceNotFoundException("Reservation not found: " + id));
		return reschedule(reservation, request);
	}

	public ReservationResponse updateReservationStatusByAdmin(Long id, UpdateReservationStatusRequest request) {
		Objects.requireNonNull(request, "request must not be null");
		ReservationStatus requested = requireStatus(request.getStatus());
		Reservation reservation = reservationRepository.findByIdForUpdate(requireId(id))
				.orElseThrow(() -> new ResourceNotFoundException("Reservation not found: " + id));
		return applyAdminStatus(reservation, requested);
	}

	@Transactional(readOnly = true)
	public ReservationAvailabilityResponse findAvailableTables(
			LocalDateTime startTime,
			LocalDateTime endTime,
			Integer guestCount) {
		validateInterval(startTime, endTime, true);
		Integer guests = requireGuestCount(guestCount);
		List<AvailableTableResponse> tables = diningTableRepository.findByActiveTrueOrderByTableNumberAsc().stream()
				.filter(table -> table.getStatus() != DiningTableStatus.OUT_OF_SERVICE)
				.filter(table -> table.getCapacity() >= guests)
				.filter(table -> !reservationRepository.existsConfirmedConflict(
						table.getId(), startTime, endTime))
				.sorted(Comparator
						.comparing(DiningTable::getCapacity)
						.thenComparing(DiningTable::getTableNumber))
				.map(table -> new AvailableTableResponse(
						table.getId(),
						table.getTableNumber(),
						table.getDisplayName(),
						table.getCapacity()))
				.toList();
		return new ReservationAvailabilityResponse(startTime, endTime, guests, tables);
	}

	@Transactional(readOnly = true)
	public List<ReservationScheduleEntryResponse> getSchedule(
			LocalDateTime from,
			LocalDateTime to,
			Long tableId,
			ReservationStatus status,
			boolean waiterContext) {
		validateRange(from, to);
		Collection<ReservationStatus> statuses;
		if (status != null) {
			statuses = EnumSet.of(status);
		}
		else if (waiterContext) {
			statuses = WAITER_DEFAULT_STATUSES;
		}
		else {
			statuses = EnumSet.allOf(ReservationStatus.class);
		}
		return reservationRepository.findSchedule(from, to, tableId, null, statuses).stream()
				.map(this::toScheduleEntry)
				.toList();
	}

	private ReservationResponse createReservation(
			AppUser client,
			Long diningTableId,
			LocalDateTime startTime,
			LocalDateTime endTime,
			Integer guestCount,
			String notes) {
		validateInterval(startTime, endTime, true);
		Integer guests = requireGuestCount(guestCount);
		String normalizedNotes = normalizeNotes(notes);

		DiningTable table = lockAndValidateTable(diningTableId, guests);
		assertNoConflict(table.getId(), startTime, endTime, null);

		LocalDateTime now = LocalDateTime.now(clock);
		Reservation reservation = new Reservation(
				UUID.randomUUID().toString(),
				table,
				client,
				startTime,
				endTime,
				guests,
				normalizedNotes,
				now);
		return toResponse(reservationRepository.save(reservation));
	}

	private ReservationResponse reschedule(Reservation reservation, UpdateReservationRequest request) {
		LocalDateTime now = LocalDateTime.now(clock);
		if (reservation.getStatus() != ReservationStatus.CONFIRMED) {
			throw new BusinessRuleException("Only CONFIRMED reservations can be rescheduled");
		}
		if (!reservation.getStartTime().isAfter(now)) {
			throw new BusinessRuleException("Cannot reschedule a reservation that has already started");
		}

		validateInterval(request.getStartTime(), request.getEndTime(), true);
		Integer guests = requireGuestCount(request.getGuestCount());
		String normalizedNotes = normalizeNotes(request.getNotes());

		DiningTable table = lockAndValidateTable(request.getDiningTableId(), guests);
		assertNoConflict(table.getId(), request.getStartTime(), request.getEndTime(), reservation.getId());

		reservation.setDiningTable(table);
		reservation.setStartTime(request.getStartTime());
		reservation.setEndTime(request.getEndTime());
		reservation.setGuestCount(guests);
		reservation.setNotes(normalizedNotes);
		reservation.setUpdatedAt(now);
		return toResponse(reservation);
	}

	private ReservationResponse cancelByClient(Reservation reservation) {
		LocalDateTime now = LocalDateTime.now(clock);
		if (reservation.getStatus() == ReservationStatus.CANCELLED) {
			return toResponse(reservation);
		}
		if (reservation.getStatus() != ReservationStatus.CONFIRMED) {
			throw new BusinessRuleException("Only CONFIRMED reservations can be cancelled by the client");
		}
		if (!reservation.getStartTime().isAfter(now)) {
			throw new BusinessRuleException("Cannot cancel a reservation that has already started");
		}
		reservation.setStatus(ReservationStatus.CANCELLED);
		reservation.setUpdatedAt(now);
		return toResponse(reservation);
	}

	private ReservationResponse applyAdminStatus(Reservation reservation, ReservationStatus requested) {
		LocalDateTime now = LocalDateTime.now(clock);
		if (reservation.getStatus() == requested) {
			return toResponse(reservation);
		}
		if (reservation.getStatus() != ReservationStatus.CONFIRMED) {
			throw new BusinessRuleException("Terminal reservation status cannot be changed");
		}

		if (requested == ReservationStatus.CANCELLED) {
			if (!reservation.getStartTime().isAfter(now)) {
				throw new BusinessRuleException("Cannot cancel a reservation that has already started");
			}
		}
		else if (requested == ReservationStatus.COMPLETED) {
			if (now.isBefore(reservation.getEndTime())) {
				throw new BusinessRuleException("COMPLETED can only be set after reservation endTime");
			}
		}
		else if (requested == ReservationStatus.NO_SHOW) {
			if (now.isBefore(reservation.getStartTime())) {
				throw new BusinessRuleException("NO_SHOW can only be set after reservation startTime");
			}
		}
		else if (requested == ReservationStatus.CONFIRMED) {
			throw new BusinessRuleException("Cannot change status back to CONFIRMED");
		}
		else {
			throw new InvalidRequestException("Unsupported reservation status: " + requested);
		}

		reservation.setStatus(requested);
		reservation.setUpdatedAt(now);
		return toResponse(reservation);
	}

	private DiningTable lockAndValidateTable(Long diningTableId, Integer guestCount) {
		if (diningTableId == null) {
			throw new InvalidRequestException("diningTableId must be provided");
		}
		DiningTable table = diningTableRepository.findByIdForUpdate(diningTableId)
				.orElseThrow(() -> new ResourceNotFoundException("Dining table not found: " + diningTableId));
		if (!table.isActive()) {
			throw new BusinessRuleException("Dining table is inactive");
		}
		if (table.getStatus() == DiningTableStatus.OUT_OF_SERVICE) {
			throw new BusinessRuleException("Dining table is OUT_OF_SERVICE");
		}
		if (guestCount > table.getCapacity()) {
			throw new InvalidRequestException("guestCount exceeds dining table capacity");
		}
		return table;
	}

	private void assertNoConflict(
			Long tableId,
			LocalDateTime startTime,
			LocalDateTime endTime,
			Long excludeId) {
		boolean conflict = excludeId == null
				? reservationRepository.existsConfirmedConflict(tableId, startTime, endTime)
				: reservationRepository.existsConfirmedConflictExcluding(tableId, startTime, endTime, excludeId);
		if (conflict) {
			throw new BusinessRuleException("Dining table already has a confirmed reservation in this time range");
		}
	}

	private void validateInterval(LocalDateTime startTime, LocalDateTime endTime, boolean requireFutureStart) {
		if (startTime == null) {
			throw new InvalidRequestException("startTime must be provided");
		}
		if (endTime == null) {
			throw new InvalidRequestException("endTime must be provided");
		}
		if (!startTime.isBefore(endTime)) {
			throw new InvalidRequestException("startTime must be before endTime");
		}
		if (requireFutureStart) {
			LocalDateTime now = LocalDateTime.now(clock);
			if (!startTime.isAfter(now)) {
				throw new InvalidRequestException("startTime must be in the future");
			}
		}
	}

	private void validateRange(LocalDateTime from, LocalDateTime to) {
		if (from == null) {
			throw new InvalidRequestException("from must be provided");
		}
		if (to == null) {
			throw new InvalidRequestException("to must be provided");
		}
		if (!from.isBefore(to)) {
			throw new InvalidRequestException("from must be before to");
		}
	}

	private Integer requireGuestCount(Integer guestCount) {
		if (guestCount == null) {
			throw new InvalidRequestException("guestCount must be provided");
		}
		if (guestCount < 1) {
			throw new InvalidRequestException("guestCount must be at least 1");
		}
		return guestCount;
	}

	private String normalizeNotes(String notes) {
		if (notes == null || notes.isBlank()) {
			return null;
		}
		String trimmed = notes.trim();
		if (trimmed.length() > MAX_NOTES_LENGTH) {
			throw new InvalidRequestException("notes must be at most " + MAX_NOTES_LENGTH + " characters");
		}
		return trimmed;
	}

	private Long requireId(Long id) {
		if (id == null) {
			throw new InvalidRequestException("Reservation id must be provided");
		}
		return id;
	}

	private ReservationStatus requireStatus(String status) {
		if (status == null || status.isBlank()) {
			throw new InvalidRequestException("status must be provided");
		}
		try {
			return ReservationStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
		}
		catch (IllegalArgumentException ex) {
			throw new InvalidRequestException("Unknown reservation status: " + status.trim());
		}
	}

	private AppUser requireAuthenticatedClient() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !authentication.isAuthenticated()
				|| authentication.getName() == null
				|| "anonymousUser".equals(authentication.getName())) {
			throw new AccessDeniedException("Authentication required");
		}
		AppUser user = appUserRepository.findByEmail(authentication.getName())
				.orElseThrow(() -> new AccessDeniedException("Authenticated user was not found"));
		if (!user.isEnabled()) {
			throw new AccessDeniedException("Authenticated user is disabled");
		}
		boolean isClient = user.getRoles().stream().anyMatch(role -> role.getName() == RoleName.CLIENT);
		if (!isClient) {
			throw new AccessDeniedException("Only CLIENT can manage personal reservations");
		}
		return user;
	}

	private AppUser loadClientForAdmin(Long clientId) {
		if (clientId == null) {
			throw new InvalidRequestException("clientId must be provided");
		}
		AppUser client = appUserRepository.findById(clientId)
				.orElseThrow(() -> new ResourceNotFoundException("Client not found: " + clientId));
		if (!client.isEnabled()) {
			throw new BusinessRuleException("Client user is disabled");
		}
		boolean isClient = client.getRoles().stream().anyMatch(role -> role.getName() == RoleName.CLIENT);
		if (!isClient) {
			throw new BusinessRuleException("Selected user does not have CLIENT role");
		}
		return client;
	}

	private ReservationResponse toResponse(Reservation reservation) {
		DiningTable table = reservation.getDiningTable();
		AppUser client = reservation.getClient();
		return new ReservationResponse(
				reservation.getId(),
				reservation.getReservationNumber(),
				table.getId(),
				table.getTableNumber(),
				table.getDisplayName(),
				client.getId(),
				client.getFullName(),
				client.getEmail(),
				reservation.getStartTime(),
				reservation.getEndTime(),
				reservation.getGuestCount(),
				reservation.getStatus().name(),
				reservation.getNotes(),
				reservation.getCreatedAt(),
				reservation.getUpdatedAt());
	}

	private ReservationScheduleEntryResponse toScheduleEntry(Reservation reservation) {
		return new ReservationScheduleEntryResponse(
				reservation.getId(),
				reservation.getReservationNumber(),
				reservation.getDiningTable().getId(),
				reservation.getDiningTable().getTableNumber(),
				reservation.getClient().getId(),
				reservation.getClient().getFullName(),
				reservation.getStartTime(),
				reservation.getEndTime(),
				reservation.getGuestCount(),
				reservation.getStatus().name());
	}
}
