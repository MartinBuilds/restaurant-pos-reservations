package bg.martinandonov.restaurant.diningtable.service;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

@Service
@Transactional
public class DiningTableService {

	private static final int MAX_CAPACITY = 50;
	private static final int MAX_DISPLAY_NAME_LENGTH = 100;

	private final DiningTableRepository diningTableRepository;

	public DiningTableService(DiningTableRepository diningTableRepository) {
		this.diningTableRepository = diningTableRepository;
	}

	public DiningTableResponse createTable(CreateDiningTableRequest request) {
		Objects.requireNonNull(request, "request must not be null");
		Integer tableNumber = requireTableNumber(request.getTableNumber());
		Integer capacity = requireCapacity(request.getCapacity());
		String displayName = normalizeDisplayName(request.getDisplayName());

		if (diningTableRepository.existsByTableNumber(tableNumber)) {
			throw new BusinessRuleException("A dining table with this number already exists");
		}

		DiningTable table = new DiningTable(tableNumber, displayName, capacity);
		return toResponse(diningTableRepository.save(table));
	}

	@Transactional(readOnly = true)
	public List<DiningTableResponse> getAllTables() {
		return diningTableRepository.findAllByOrderByTableNumberAsc().stream()
				.map(this::toResponse)
				.toList();
	}

	@Transactional(readOnly = true)
	public List<DiningTableResponse> getActiveTables() {
		return diningTableRepository.findByActiveTrueOrderByTableNumberAsc().stream()
				.map(this::toResponse)
				.toList();
	}

	@Transactional(readOnly = true)
	public DiningTableResponse getTableById(Long id) {
		return toResponse(findTable(id));
	}

	@Transactional(readOnly = true)
	public DiningTableResponse getActiveTableById(Long id) {
		DiningTable table = findTable(id);
		if (!table.isActive()) {
			throw new ResourceNotFoundException("Dining table not found: " + id);
		}
		return toResponse(table);
	}

	@Transactional(readOnly = true)
	public List<DiningTableResponse> getActiveTablesByStatus(DiningTableStatus status) {
		Objects.requireNonNull(status, "status must not be null");
		return diningTableRepository.findByActiveTrueAndStatusOrderByTableNumberAsc(status).stream()
				.map(this::toResponse)
				.toList();
	}

	public DiningTableResponse updateTable(Long id, UpdateDiningTableRequest request) {
		Objects.requireNonNull(request, "request must not be null");
		DiningTable table = findTable(id);
		Integer tableNumber = requireTableNumber(request.getTableNumber());
		Integer capacity = requireCapacity(request.getCapacity());
		String displayName = normalizeDisplayName(request.getDisplayName());

		if (diningTableRepository.existsByTableNumberAndIdNot(tableNumber, id)) {
			throw new BusinessRuleException("A dining table with this number already exists");
		}

		table.setTableNumber(tableNumber);
		table.setDisplayName(displayName);
		table.setCapacity(capacity);
		return toResponse(table);
	}

	public DiningTableResponse updateTableStatus(Long id, UpdateDiningTableStatusRequest request) {
		Objects.requireNonNull(request, "request must not be null");
		DiningTableStatus status = requireStatus(request.getStatus());
		DiningTable table = diningTableRepository.findByIdForUpdate(id)
				.orElseThrow(() -> new ResourceNotFoundException("Dining table not found: " + id));
		applyStatus(table, status, false);
		return toResponse(table);
	}

	public DiningTableResponse updateActiveTableStatus(Long id, UpdateDiningTableStatusRequest request) {
		Objects.requireNonNull(request, "request must not be null");
		DiningTableStatus status = requireStatus(request.getStatus());
		if (status == DiningTableStatus.OUT_OF_SERVICE) {
			throw new BusinessRuleException("Waiters cannot set a table to OUT_OF_SERVICE");
		}
		DiningTable table = diningTableRepository.findByIdForUpdate(id)
				.orElseThrow(() -> new ResourceNotFoundException("Dining table not found: " + id));
		if (!table.isActive()) {
			throw new ResourceNotFoundException("Dining table not found: " + id);
		}
		applyStatus(table, status, true);
		return toResponse(table);
	}

	public DiningTableResponse updateTableActiveState(Long id, UpdateDiningTableActiveRequest request) {
		Objects.requireNonNull(request, "request must not be null");
		if (request.getActive() == null) {
			throw new InvalidRequestException("active must be provided");
		}

		DiningTable table = diningTableRepository.findByIdForUpdate(id)
				.orElseThrow(() -> new ResourceNotFoundException("Dining table not found: " + id));

		if (request.getActive()) {
			table.setActive(true);
			table.setStatus(DiningTableStatus.AVAILABLE);
		}
		else {
			table.setActive(false);
			table.setStatus(DiningTableStatus.OUT_OF_SERVICE);
		}
		return toResponse(table);
	}

	private void applyStatus(DiningTable table, DiningTableStatus status, boolean waiterContext) {
		if (!table.isActive()) {
			if (status != DiningTableStatus.OUT_OF_SERVICE) {
				throw new BusinessRuleException("Inactive tables can only remain OUT_OF_SERVICE");
			}
			table.setStatus(DiningTableStatus.OUT_OF_SERVICE);
			return;
		}

		if (waiterContext && status == DiningTableStatus.OUT_OF_SERVICE) {
			throw new BusinessRuleException("Waiters cannot set a table to OUT_OF_SERVICE");
		}

		table.setStatus(status);
	}

	private DiningTable findTable(Long id) {
		if (id == null) {
			throw new InvalidRequestException("Dining table id must be provided");
		}
		return diningTableRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Dining table not found: " + id));
	}

	private Integer requireTableNumber(Integer tableNumber) {
		if (tableNumber == null) {
			throw new InvalidRequestException("tableNumber must be provided");
		}
		if (tableNumber <= 0) {
			throw new InvalidRequestException("tableNumber must be greater than 0");
		}
		return tableNumber;
	}

	private Integer requireCapacity(Integer capacity) {
		if (capacity == null) {
			throw new InvalidRequestException("capacity must be provided");
		}
		if (capacity < 1 || capacity > MAX_CAPACITY) {
			throw new InvalidRequestException("capacity must be between 1 and " + MAX_CAPACITY);
		}
		return capacity;
	}

	private String normalizeDisplayName(String displayName) {
		if (displayName == null || displayName.isBlank()) {
			return null;
		}
		String trimmed = displayName.trim();
		if (trimmed.length() > MAX_DISPLAY_NAME_LENGTH) {
			throw new InvalidRequestException(
					"displayName must be at most " + MAX_DISPLAY_NAME_LENGTH + " characters");
		}
		return trimmed;
	}

	private DiningTableStatus requireStatus(String status) {
		if (status == null || status.isBlank()) {
			throw new InvalidRequestException("status must be provided");
		}
		try {
			return DiningTableStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
		}
		catch (IllegalArgumentException ex) {
			throw new InvalidRequestException("Unknown dining table status: " + status.trim());
		}
	}

	private DiningTableResponse toResponse(DiningTable table) {
		return new DiningTableResponse(
				table.getId(),
				table.getTableNumber(),
				table.getDisplayName(),
				table.getCapacity(),
				table.getStatus().name(),
				table.isActive(),
				table.getVersion());
	}
}
