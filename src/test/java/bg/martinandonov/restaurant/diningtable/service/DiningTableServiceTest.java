package bg.martinandonov.restaurant.diningtable.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import bg.martinandonov.restaurant.common.exception.BusinessRuleException;
import bg.martinandonov.restaurant.common.exception.InvalidRequestException;
import bg.martinandonov.restaurant.common.exception.ResourceNotFoundException;
import bg.martinandonov.restaurant.diningtable.dto.CreateDiningTableRequest;
import bg.martinandonov.restaurant.diningtable.dto.DiningTableResponse;
import bg.martinandonov.restaurant.diningtable.dto.UpdateDiningTableActiveRequest;
import bg.martinandonov.restaurant.diningtable.dto.UpdateDiningTableRequest;
import bg.martinandonov.restaurant.diningtable.dto.UpdateDiningTableStatusRequest;
import bg.martinandonov.restaurant.diningtable.entity.DiningTable;
import bg.martinandonov.restaurant.diningtable.entity.DiningTableStatus;
import bg.martinandonov.restaurant.diningtable.repository.DiningTableRepository;

@ExtendWith(MockitoExtension.class)
class DiningTableServiceTest {

	@Mock
	private DiningTableRepository diningTableRepository;

	@Mock
	private DiningTableOperationalGuard diningTableOperationalGuard;

	@InjectMocks
	private DiningTableService diningTableService;

	@Test
	void createTableSetsAvailableAndActive() {
		CreateDiningTableRequest request = createRequest(5, "  Window  ", 4);
		when(diningTableRepository.existsByTableNumber(5)).thenReturn(false);
		when(diningTableRepository.save(any(DiningTable.class))).thenAnswer(invocation -> {
			DiningTable table = invocation.getArgument(0);
			ReflectionTestUtils.setField(table, "id", 1L);
			ReflectionTestUtils.setField(table, "version", 0L);
			return table;
		});

		DiningTableResponse response = diningTableService.createTable(request);

		ArgumentCaptor<DiningTable> captor = ArgumentCaptor.forClass(DiningTable.class);
		verify(diningTableRepository).save(captor.capture());
		assertThat(captor.getValue().getStatus()).isEqualTo(DiningTableStatus.AVAILABLE);
		assertThat(captor.getValue().isActive()).isTrue();
		assertThat(captor.getValue().getDisplayName()).isEqualTo("Window");
		assertThat(response.getStatus()).isEqualTo("AVAILABLE");
		assertThat(response.isActive()).isTrue();
	}

	@Test
	void createRejectsInvalidTableNumber() {
		CreateDiningTableRequest request = createRequest(0, "A", 4);

		assertThatThrownBy(() -> diningTableService.createTable(request))
				.isInstanceOf(InvalidRequestException.class)
				.hasMessageContaining("tableNumber");
		verify(diningTableRepository, never()).save(any());
	}

	@Test
	void createRejectsInvalidCapacity() {
		CreateDiningTableRequest request = createRequest(1, "A", 51);

		assertThatThrownBy(() -> diningTableService.createTable(request))
				.isInstanceOf(InvalidRequestException.class)
				.hasMessageContaining("capacity");
	}

	@Test
	void createRejectsDuplicateTableNumber() {
		CreateDiningTableRequest request = createRequest(1, "A", 4);
		when(diningTableRepository.existsByTableNumber(1)).thenReturn(true);

		assertThatThrownBy(() -> diningTableService.createTable(request))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("already exists");
	}

	@Test
	void blankDisplayNameBecomesNull() {
		CreateDiningTableRequest request = createRequest(2, "   ", 2);
		when(diningTableRepository.existsByTableNumber(2)).thenReturn(false);
		when(diningTableRepository.save(any(DiningTable.class))).thenAnswer(invocation -> {
			DiningTable table = invocation.getArgument(0);
			ReflectionTestUtils.setField(table, "id", 2L);
			ReflectionTestUtils.setField(table, "version", 0L);
			return table;
		});

		diningTableService.createTable(request);

		ArgumentCaptor<DiningTable> captor = ArgumentCaptor.forClass(DiningTable.class);
		verify(diningTableRepository).save(captor.capture());
		assertThat(captor.getValue().getDisplayName()).isNull();
	}

	@Test
	void updateDoesNotChangeStatusOrActive() {
		DiningTable table = existingTable(1L, 3, "Patio", 4, DiningTableStatus.OCCUPIED, true);
		when(diningTableRepository.findById(1L)).thenReturn(Optional.of(table));
		when(diningTableRepository.existsByTableNumberAndIdNot(7, 1L)).thenReturn(false);

		UpdateDiningTableRequest request = new UpdateDiningTableRequest();
		request.setTableNumber(7);
		request.setDisplayName("Garden");
		request.setCapacity(6);

		DiningTableResponse response = diningTableService.updateTable(1L, request);

		assertThat(response.getTableNumber()).isEqualTo(7);
		assertThat(response.getDisplayName()).isEqualTo("Garden");
		assertThat(response.getCapacity()).isEqualTo(6);
		assertThat(response.getStatus()).isEqualTo("OCCUPIED");
		assertThat(response.isActive()).isTrue();
	}

	@Test
	void updateRejectsDuplicateNumber() {
		DiningTable table = existingTable(1L, 3, null, 4, DiningTableStatus.AVAILABLE, true);
		when(diningTableRepository.findById(1L)).thenReturn(Optional.of(table));
		when(diningTableRepository.existsByTableNumberAndIdNot(9, 1L)).thenReturn(true);

		UpdateDiningTableRequest request = new UpdateDiningTableRequest();
		request.setTableNumber(9);
		request.setCapacity(4);

		assertThatThrownBy(() -> diningTableService.updateTable(1L, request))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("already exists");
	}

	@Test
	void missingTableReturns404() {
		when(diningTableRepository.findById(99L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> diningTableService.getTableById(99L))
				.isInstanceOf(ResourceNotFoundException.class)
				.hasMessageContaining("99");
	}

	@Test
	void deactivateSetsOutOfService() {
		DiningTable table = existingTable(1L, 3, null, 4, DiningTableStatus.OCCUPIED, true);
		when(diningTableRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(table));

		UpdateDiningTableActiveRequest request = new UpdateDiningTableActiveRequest();
		request.setActive(false);

		DiningTableResponse response = diningTableService.updateTableActiveState(1L, request);

		assertThat(response.isActive()).isFalse();
		assertThat(response.getStatus()).isEqualTo("OUT_OF_SERVICE");
	}

	@Test
	void activateSetsAvailable() {
		DiningTable table = existingTable(1L, 3, null, 4, DiningTableStatus.OUT_OF_SERVICE, false);
		when(diningTableRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(table));

		UpdateDiningTableActiveRequest request = new UpdateDiningTableActiveRequest();
		request.setActive(true);

		DiningTableResponse response = diningTableService.updateTableActiveState(1L, request);

		assertThat(response.isActive()).isTrue();
		assertThat(response.getStatus()).isEqualTo("AVAILABLE");
	}

	@Test
	void inactiveTableRejectsAvailable() {
		DiningTable table = existingTable(1L, 3, null, 4, DiningTableStatus.OUT_OF_SERVICE, false);
		when(diningTableRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(table));

		assertThatThrownBy(() -> diningTableService.updateTableStatus(1L, statusRequest("AVAILABLE")))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("OUT_OF_SERVICE");
	}

	@Test
	void inactiveTableRejectsOccupied() {
		DiningTable table = existingTable(1L, 3, null, 4, DiningTableStatus.OUT_OF_SERVICE, false);
		when(diningTableRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(table));

		assertThatThrownBy(() -> diningTableService.updateTableStatus(1L, statusRequest("OCCUPIED")))
				.isInstanceOf(BusinessRuleException.class);
	}

	@Test
	void inactiveTableRejectsReserved() {
		DiningTable table = existingTable(1L, 3, null, 4, DiningTableStatus.OUT_OF_SERVICE, false);
		when(diningTableRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(table));

		assertThatThrownBy(() -> diningTableService.updateTableStatus(1L, statusRequest("RESERVED")))
				.isInstanceOf(BusinessRuleException.class);
	}

	@Test
	void activeTableCanBecomeOutOfService() {
		DiningTable table = existingTable(1L, 3, null, 4, DiningTableStatus.AVAILABLE, true);
		when(diningTableRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(table));

		DiningTableResponse response = diningTableService.updateTableStatus(1L, statusRequest("OUT_OF_SERVICE"));

		assertThat(response.getStatus()).isEqualTo("OUT_OF_SERVICE");
		assertThat(response.isActive()).isTrue();
	}

	@Test
	void activeListsAreDeterministicAndExcludeInactive() {
		DiningTable active = existingTable(1L, 2, null, 2, DiningTableStatus.AVAILABLE, true);
		when(diningTableRepository.findByActiveTrueOrderByTableNumberAsc()).thenReturn(List.of(active));

		List<DiningTableResponse> tables = diningTableService.getActiveTables();

		assertThat(tables).hasSize(1);
		assertThat(tables.get(0).getTableNumber()).isEqualTo(2);
		assertThat(tables.get(0).isActive()).isTrue();
	}

	@Test
	void waiterSeesOnlyActiveTablesByStatus() {
		DiningTable available = existingTable(1L, 1, null, 2, DiningTableStatus.AVAILABLE, true);
		when(diningTableRepository.findByActiveTrueAndStatusOrderByTableNumberAsc(DiningTableStatus.AVAILABLE))
				.thenReturn(List.of(available));

		List<DiningTableResponse> tables =
				diningTableService.getActiveTablesByStatus(DiningTableStatus.AVAILABLE);

		assertThat(tables).extracting(DiningTableResponse::getStatus).containsExactly("AVAILABLE");
	}

	@Test
	void waiterCanSetAvailableOccupiedReserved() {
		DiningTable table = existingTable(1L, 1, null, 2, DiningTableStatus.AVAILABLE, true);
		when(diningTableRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(table));

		assertThat(diningTableService.updateActiveTableStatus(1L, statusRequest("OCCUPIED")).getStatus())
				.isEqualTo("OCCUPIED");

		assertThat(diningTableService.updateActiveTableStatus(1L, statusRequest("RESERVED")).getStatus())
				.isEqualTo("RESERVED");

		assertThat(diningTableService.updateActiveTableStatus(1L, statusRequest("AVAILABLE")).getStatus())
				.isEqualTo("AVAILABLE");
	}

	@Test
	void waiterCannotSetOutOfService() {
		assertThatThrownBy(() -> diningTableService.updateActiveTableStatus(1L, statusRequest("OUT_OF_SERVICE")))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("OUT_OF_SERVICE");
		verify(diningTableRepository, never()).findByIdForUpdate(1L);
	}

	@Test
	void waiterDoesNotSeeInactiveTableById() {
		DiningTable inactive = existingTable(1L, 1, null, 2, DiningTableStatus.OUT_OF_SERVICE, false);
		when(diningTableRepository.findById(1L)).thenReturn(Optional.of(inactive));

		assertThatThrownBy(() -> diningTableService.getActiveTableById(1L))
				.isInstanceOf(ResourceNotFoundException.class);
	}

	@Test
	void openOrderBlocksAdminSettingAvailable() {
		DiningTable table = existingTable(1L, 3, null, 4, DiningTableStatus.OCCUPIED, true);
		when(diningTableRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(table));
		when(diningTableOperationalGuard.hasOpenOrder(1L)).thenReturn(true);

		assertThatThrownBy(() -> diningTableService.updateTableStatus(1L, statusRequest("AVAILABLE")))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("open order");
	}

	@Test
	void openOrderBlocksAdminSettingReserved() {
		DiningTable table = existingTable(1L, 3, null, 4, DiningTableStatus.OCCUPIED, true);
		when(diningTableRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(table));
		when(diningTableOperationalGuard.hasOpenOrder(1L)).thenReturn(true);

		assertThatThrownBy(() -> diningTableService.updateTableStatus(1L, statusRequest("RESERVED")))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("open order");
	}

	@Test
	void openOrderBlocksDeactivate() {
		DiningTable table = existingTable(1L, 3, null, 4, DiningTableStatus.OCCUPIED, true);
		when(diningTableRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(table));
		when(diningTableOperationalGuard.hasOpenOrder(1L)).thenReturn(true);

		UpdateDiningTableActiveRequest request = new UpdateDiningTableActiveRequest();
		request.setActive(false);

		assertThatThrownBy(() -> diningTableService.updateTableActiveState(1L, request))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("open order");
	}

	@Test
	void openOrderBlocksWaiterSettingAvailable() {
		DiningTable table = existingTable(1L, 1, null, 2, DiningTableStatus.OCCUPIED, true);
		when(diningTableRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(table));
		when(diningTableOperationalGuard.hasOpenOrder(1L)).thenReturn(true);

		assertThatThrownBy(() -> diningTableService.updateActiveTableStatus(1L, statusRequest("AVAILABLE")))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("open order");
	}

	@Test
	void openOrderBlocksWaiterSettingReserved() {
		DiningTable table = existingTable(1L, 1, null, 2, DiningTableStatus.OCCUPIED, true);
		when(diningTableRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(table));
		when(diningTableOperationalGuard.hasOpenOrder(1L)).thenReturn(true);

		assertThatThrownBy(() -> diningTableService.updateActiveTableStatus(1L, statusRequest("RESERVED")))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("open order");
	}

	private CreateDiningTableRequest createRequest(Integer number, String name, Integer capacity) {
		CreateDiningTableRequest request = new CreateDiningTableRequest();
		request.setTableNumber(number);
		request.setDisplayName(name);
		request.setCapacity(capacity);
		return request;
	}

	private UpdateDiningTableStatusRequest statusRequest(String status) {
		UpdateDiningTableStatusRequest request = new UpdateDiningTableStatusRequest();
		request.setStatus(status);
		return request;
	}

	private DiningTable existingTable(
			Long id,
			Integer number,
			String displayName,
			Integer capacity,
			DiningTableStatus status,
			boolean active) {
		DiningTable table = new DiningTable(number, displayName, capacity);
		ReflectionTestUtils.setField(table, "id", id);
		ReflectionTestUtils.setField(table, "version", 0L);
		table.setStatus(status);
		table.setActive(active);
		return table;
	}
}
