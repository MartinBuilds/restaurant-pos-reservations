package bg.martinandonov.restaurant.reservation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import bg.martinandonov.restaurant.common.exception.BusinessRuleException;
import bg.martinandonov.restaurant.common.exception.InvalidRequestException;
import bg.martinandonov.restaurant.common.exception.ResourceNotFoundException;
import bg.martinandonov.restaurant.diningtable.entity.DiningTable;
import bg.martinandonov.restaurant.diningtable.entity.DiningTableStatus;
import bg.martinandonov.restaurant.diningtable.repository.DiningTableRepository;
import bg.martinandonov.restaurant.reservation.dto.CreateAdminReservationRequest;
import bg.martinandonov.restaurant.reservation.dto.CreateClientReservationRequest;
import bg.martinandonov.restaurant.reservation.dto.ReservationAvailabilityResponse;
import bg.martinandonov.restaurant.reservation.dto.ReservationResponse;
import bg.martinandonov.restaurant.reservation.dto.UpdateReservationRequest;
import bg.martinandonov.restaurant.reservation.dto.UpdateReservationStatusRequest;
import bg.martinandonov.restaurant.reservation.entity.Reservation;
import bg.martinandonov.restaurant.reservation.entity.ReservationStatus;
import bg.martinandonov.restaurant.reservation.repository.ReservationRepository;
import bg.martinandonov.restaurant.user.entity.AppUser;
import bg.martinandonov.restaurant.user.entity.Role;
import bg.martinandonov.restaurant.user.entity.RoleName;
import bg.martinandonov.restaurant.user.repository.AppUserRepository;

@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {

	private static final ZoneId ZONE = ZoneId.of("Europe/Sofia");
	private static final Instant FIXED_INSTANT = Instant.parse("2026-08-05T10:00:00Z");
	private static final LocalDateTime NOW = LocalDateTime.ofInstant(FIXED_INSTANT, ZONE);

	@Mock
	private ReservationRepository reservationRepository;

	@Mock
	private DiningTableRepository diningTableRepository;

	@Mock
	private AppUserRepository appUserRepository;

	private ReservationService reservationService;

	private AppUser client;
	private DiningTable table;

	@BeforeEach
	void setUp() {
		Clock fixedClock = Clock.fixed(FIXED_INSTANT, ZONE);
		reservationService = new ReservationService(
				reservationRepository, diningTableRepository, appUserRepository, fixedClock);

		client = new AppUser("client@example.com", "hash", "Client One", true);
		ReflectionTestUtils.setField(client, "id", 10L);
		client.setRoles(Set.of(new Role(RoleName.CLIENT)));

		table = new DiningTable(5, "Window", 4);
		ReflectionTestUtils.setField(table, "id", 1L);
		table.setStatus(DiningTableStatus.AVAILABLE);

		SecurityContext context = SecurityContextHolder.createEmptyContext();
		Authentication authentication = org.mockito.Mockito.mock(Authentication.class);
		org.mockito.Mockito.lenient().when(authentication.isAuthenticated()).thenReturn(true);
		org.mockito.Mockito.lenient().when(authentication.getName()).thenReturn("client@example.com");
		context.setAuthentication(authentication);
		SecurityContextHolder.setContext(context);
		org.mockito.Mockito.lenient().when(appUserRepository.findByEmail("client@example.com"))
				.thenReturn(Optional.of(client));
	}

	@AfterEach
	void tearDown() {
		SecurityContextHolder.clearContext();
	}

	@Test
	void createForCurrentClientCreatesConfirmedReservation() {
		LocalDateTime start = NOW.plusHours(2);
		LocalDateTime end = NOW.plusHours(4);
		stubLockTable();
		when(reservationRepository.existsConfirmedConflict(1L, start, end)).thenReturn(false);
		when(reservationRepository.save(any(Reservation.class))).thenAnswer(invocation -> {
			Reservation reservation = invocation.getArgument(0);
			ReflectionTestUtils.setField(reservation, "id", 100L);
			return reservation;
		});

		ReservationResponse response = reservationService.createForCurrentClient(createClientRequest(1L, start, end, 2, "  window  "));

		ArgumentCaptor<Reservation> captor = ArgumentCaptor.forClass(Reservation.class);
		verify(reservationRepository).save(captor.capture());
		assertThat(captor.getValue().getStatus()).isEqualTo(ReservationStatus.CONFIRMED);
		assertThat(captor.getValue().getReservationNumber()).isNotBlank().hasSize(36);
		assertThat(captor.getValue().getNotes()).isEqualTo("window");
		assertThat(captor.getValue().getClient().getId()).isEqualTo(10L);
		assertThat(table.getStatus()).isEqualTo(DiningTableStatus.AVAILABLE);
		assertThat(response.getStatus()).isEqualTo("CONFIRMED");
	}

	@Test
	void blankNotesBecomeNull() {
		LocalDateTime start = NOW.plusHours(2);
		LocalDateTime end = NOW.plusHours(4);
		stubLockTable();
		when(reservationRepository.existsConfirmedConflict(1L, start, end)).thenReturn(false);
		when(reservationRepository.save(any(Reservation.class))).thenAnswer(invocation -> invocation.getArgument(0));

		reservationService.createForCurrentClient(createClientRequest(1L, start, end, 2, "   "));

		ArgumentCaptor<Reservation> captor = ArgumentCaptor.forClass(Reservation.class);
		verify(reservationRepository).save(captor.capture());
		assertThat(captor.getValue().getNotes()).isNull();
	}

	@Test
	void rejectsInvalidIntervalAndPastStart() {
		assertThatThrownBy(() -> reservationService.createForCurrentClient(
				createClientRequest(1L, NOW.plusHours(3), NOW.plusHours(2), 2, null)))
				.isInstanceOf(InvalidRequestException.class)
				.hasMessageContaining("before endTime");

		assertThatThrownBy(() -> reservationService.createForCurrentClient(
				createClientRequest(1L, NOW.minusHours(1), NOW.plusHours(1), 2, null)))
				.isInstanceOf(InvalidRequestException.class)
				.hasMessageContaining("future");

		verify(diningTableRepository, never()).findByIdForUpdate(any());
	}

	@Test
	void rejectsInvalidGuestCountAndCapacity() {
		assertThatThrownBy(() -> reservationService.createForCurrentClient(
				createClientRequest(1L, NOW.plusHours(2), NOW.plusHours(3), 0, null)))
				.isInstanceOf(InvalidRequestException.class)
				.hasMessageContaining("guestCount");

		stubLockTable();
		assertThatThrownBy(() -> reservationService.createForCurrentClient(
				createClientRequest(1L, NOW.plusHours(2), NOW.plusHours(3), 5, null)))
				.isInstanceOf(InvalidRequestException.class)
				.hasMessageContaining("capacity");
	}

	@Test
	void rejectsInactiveAndOutOfServiceTables() {
		table.setActive(false);
		stubLockTable();
		assertThatThrownBy(() -> reservationService.createForCurrentClient(
				createClientRequest(1L, NOW.plusHours(2), NOW.plusHours(3), 2, null)))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("inactive");

		table.setActive(true);
		table.setStatus(DiningTableStatus.OUT_OF_SERVICE);
		assertThatThrownBy(() -> reservationService.createForCurrentClient(
				createClientRequest(1L, NOW.plusHours(2), NOW.plusHours(3), 2, null)))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("OUT_OF_SERVICE");
	}

	@Test
	void rejectsConflictAllowsAdjacentAndDifferentTables() {
		LocalDateTime start = NOW.plusHours(2);
		LocalDateTime end = NOW.plusHours(4);
		stubLockTable();
		when(reservationRepository.existsConfirmedConflict(1L, start, end)).thenReturn(true);

		assertThatThrownBy(() -> reservationService.createForCurrentClient(
				createClientRequest(1L, start, end, 2, null)))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("confirmed reservation");

		when(reservationRepository.existsConfirmedConflict(1L, end, end.plusHours(2))).thenReturn(false);
		when(reservationRepository.save(any(Reservation.class))).thenAnswer(invocation -> {
			Reservation reservation = invocation.getArgument(0);
			ReflectionTestUtils.setField(reservation, "id", 101L);
			return reservation;
		});
		ReservationResponse adjacent = reservationService.createForCurrentClient(
				createClientRequest(1L, end, end.plusHours(2), 2, null));
		assertThat(adjacent.getId()).isEqualTo(101L);
	}

	@Test
	void occupiedTableStillAcceptsFutureReservation() {
		table.setStatus(DiningTableStatus.OCCUPIED);
		LocalDateTime start = NOW.plusHours(2);
		LocalDateTime end = NOW.plusHours(4);
		stubLockTable();
		when(reservationRepository.existsConfirmedConflict(1L, start, end)).thenReturn(false);
		when(reservationRepository.save(any(Reservation.class))).thenAnswer(invocation -> {
			Reservation reservation = invocation.getArgument(0);
			ReflectionTestUtils.setField(reservation, "id", 102L);
			return reservation;
		});

		ReservationResponse response = reservationService.createForCurrentClient(
				createClientRequest(1L, start, end, 2, null));
		assertThat(response.getStatus()).isEqualTo("CONFIRMED");
		assertThat(table.getStatus()).isEqualTo(DiningTableStatus.OCCUPIED);
	}

	@Test
	void adminCreateRequiresClientRole() {
		AppUser waiter = new AppUser("waiter@example.com", "hash", "Waiter", true);
		ReflectionTestUtils.setField(waiter, "id", 20L);
		waiter.setRoles(Set.of(new Role(RoleName.WAITER)));
		when(appUserRepository.findById(20L)).thenReturn(Optional.of(waiter));

		CreateAdminReservationRequest request = new CreateAdminReservationRequest();
		request.setClientId(20L);
		request.setDiningTableId(1L);
		request.setStartTime(NOW.plusHours(2));
		request.setEndTime(NOW.plusHours(3));
		request.setGuestCount(2);

		assertThatThrownBy(() -> reservationService.createByAdmin(request))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("CLIENT role");
	}

	@Test
	void ownershipHidesForeignReservations() {
		when(reservationRepository.findByIdAndClientId(99L, 10L)).thenReturn(Optional.empty());
		assertThatThrownBy(() -> reservationService.getCurrentClientReservationById(99L))
				.isInstanceOf(ResourceNotFoundException.class);

		Reservation foreign = existingReservation(99L, NOW.plusHours(2), NOW.plusHours(4));
		AppUser other = new AppUser("other@example.com", "hash", "Other", true);
		ReflectionTestUtils.setField(other, "id", 11L);
		ReflectionTestUtils.setField(foreign, "client", other);
		when(reservationRepository.findByIdForUpdate(99L)).thenReturn(Optional.of(foreign));

		assertThatThrownBy(() -> reservationService.updateCurrentClientReservation(99L, updateRequest(
				1L, NOW.plusHours(3), NOW.plusHours(5), 2, null)))
				.isInstanceOf(ResourceNotFoundException.class);
		assertThatThrownBy(() -> reservationService.cancelCurrentClientReservation(99L))
				.isInstanceOf(ResourceNotFoundException.class);
	}

	@Test
	void updateExcludesSelfFromConflictAndRejectsTerminal() {
		Reservation reservation = existingReservation(50L, NOW.plusHours(2), NOW.plusHours(4));
		when(reservationRepository.findByIdForUpdate(50L)).thenReturn(Optional.of(reservation));
		stubLockTable();
		when(reservationRepository.existsConfirmedConflictExcluding(
				eq(1L), eq(NOW.plusHours(3)), eq(NOW.plusHours(5)), eq(50L))).thenReturn(false);

		ReservationResponse response = reservationService.updateCurrentClientReservation(
				50L, updateRequest(1L, NOW.plusHours(3), NOW.plusHours(5), 2, "moved"));
		assertThat(response.getStartTime()).isEqualTo(NOW.plusHours(3));
		assertThat(reservation.getNotes()).isEqualTo("moved");

		reservation.setStatus(ReservationStatus.CANCELLED);
		assertThatThrownBy(() -> reservationService.updateCurrentClientReservation(
				50L, updateRequest(1L, NOW.plusHours(3), NOW.plusHours(5), 2, null)))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("CONFIRMED");
	}

	@Test
	void clientCancelIsIdempotentAndBlocksStarted() {
		Reservation reservation = existingReservation(50L, NOW.plusHours(2), NOW.plusHours(4));
		when(reservationRepository.findByIdForUpdate(50L)).thenReturn(Optional.of(reservation));

		ReservationResponse cancelled = reservationService.cancelCurrentClientReservation(50L);
		assertThat(cancelled.getStatus()).isEqualTo("CANCELLED");

		ReservationResponse again = reservationService.cancelCurrentClientReservation(50L);
		assertThat(again.getStatus()).isEqualTo("CANCELLED");

		reservation.setStatus(ReservationStatus.CONFIRMED);
		reservation.setStartTime(NOW.minusMinutes(5));
		assertThatThrownBy(() -> reservationService.cancelCurrentClientReservation(50L))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("already started");
	}

	@Test
	void adminStatusTransitions() {
		Reservation reservation = existingReservation(50L, NOW.minusHours(2), NOW.minusHours(1));
		when(reservationRepository.findByIdForUpdate(50L)).thenReturn(Optional.of(reservation));

		ReservationResponse completed = reservationService.updateReservationStatusByAdmin(
				50L, statusRequest("COMPLETED"));
		assertThat(completed.getStatus()).isEqualTo("COMPLETED");

		reservation.setStatus(ReservationStatus.CONFIRMED);
		reservation.setStartTime(NOW.minusMinutes(10));
		reservation.setEndTime(NOW.plusHours(1));
		ReservationResponse noShow = reservationService.updateReservationStatusByAdmin(
				50L, statusRequest("NO_SHOW"));
		assertThat(noShow.getStatus()).isEqualTo("NO_SHOW");

		reservation.setStatus(ReservationStatus.CONFIRMED);
		reservation.setStartTime(NOW.plusHours(1));
		reservation.setEndTime(NOW.plusHours(2));
		assertThatThrownBy(() -> reservationService.updateReservationStatusByAdmin(
				50L, statusRequest("NO_SHOW")))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("startTime");

		reservation.setStatus(ReservationStatus.CANCELLED);
		assertThatThrownBy(() -> reservationService.updateReservationStatusByAdmin(
				50L, statusRequest("COMPLETED")))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("Terminal");
	}

	@Test
	void availabilityFiltersAndOrders() {
		DiningTable small = new DiningTable(1, "S", 2);
		ReflectionTestUtils.setField(small, "id", 2L);
		DiningTable large = new DiningTable(2, "L", 6);
		ReflectionTestUtils.setField(large, "id", 3L);
		DiningTable oos = new DiningTable(3, "X", 4);
		ReflectionTestUtils.setField(oos, "id", 4L);
		oos.setStatus(DiningTableStatus.OUT_OF_SERVICE);
		DiningTable conflicted = new DiningTable(4, "C", 4);
		ReflectionTestUtils.setField(conflicted, "id", 5L);

		when(diningTableRepository.findByActiveTrueOrderByTableNumberAsc())
				.thenReturn(List.of(small, large, oos, conflicted));
		LocalDateTime start = NOW.plusHours(2);
		LocalDateTime end = NOW.plusHours(4);
		when(reservationRepository.existsConfirmedConflict(3L, start, end)).thenReturn(false);
		when(reservationRepository.existsConfirmedConflict(5L, start, end)).thenReturn(true);

		ReservationAvailabilityResponse response =
				reservationService.findAvailableTables(start, end, 3);

		assertThat(response.getAvailableTables()).extracting(t -> t.getDiningTableId())
				.containsExactly(3L);
	}

	@Test
	void scheduleUsesOverlapAndWaiterDefaultStatuses() {
		when(reservationRepository.findSchedule(
				eq(NOW), eq(NOW.plusDays(1)), isNull(), isNull(), eq(WAITER_DEFAULTS())))
				.thenReturn(List.of());

		assertThat(reservationService.getSchedule(NOW, NOW.plusDays(1), null, null, true)).isEmpty();
		verify(reservationRepository).findSchedule(
				eq(NOW), eq(NOW.plusDays(1)), isNull(), isNull(), eq(WAITER_DEFAULTS()));
	}

	@Test
	void nonClientCannotUseClientEndpoints() {
		AppUser admin = new AppUser("admin@example.com", "hash", "Admin", true);
		ReflectionTestUtils.setField(admin, "id", 1L);
		admin.setRoles(Set.of(new Role(RoleName.ADMIN)));
		when(appUserRepository.findByEmail("client@example.com")).thenReturn(Optional.of(admin));

		assertThatThrownBy(() -> reservationService.getCurrentClientReservations())
				.isInstanceOf(AccessDeniedException.class)
				.hasMessageContaining("CLIENT");
	}

	private void stubLockTable() {
		when(diningTableRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(table));
	}

	private Reservation existingReservation(Long id, LocalDateTime start, LocalDateTime end) {
		Reservation reservation = new Reservation(
				"res-" + id, table, client, start, end, 2, null, NOW.minusHours(1));
		ReflectionTestUtils.setField(reservation, "id", id);
		return reservation;
	}

	private CreateClientReservationRequest createClientRequest(
			Long tableId, LocalDateTime start, LocalDateTime end, Integer guests, String notes) {
		CreateClientReservationRequest request = new CreateClientReservationRequest();
		request.setDiningTableId(tableId);
		request.setStartTime(start);
		request.setEndTime(end);
		request.setGuestCount(guests);
		request.setNotes(notes);
		return request;
	}

	private UpdateReservationRequest updateRequest(
			Long tableId, LocalDateTime start, LocalDateTime end, Integer guests, String notes) {
		UpdateReservationRequest request = new UpdateReservationRequest();
		request.setDiningTableId(tableId);
		request.setStartTime(start);
		request.setEndTime(end);
		request.setGuestCount(guests);
		request.setNotes(notes);
		return request;
	}

	private UpdateReservationStatusRequest statusRequest(String status) {
		UpdateReservationStatusRequest request = new UpdateReservationStatusRequest();
		request.setStatus(status);
		return request;
	}

	private static EnumSet<ReservationStatus> WAITER_DEFAULTS() {
		return EnumSet.of(ReservationStatus.CONFIRMED, ReservationStatus.COMPLETED, ReservationStatus.NO_SHOW);
	}
}
