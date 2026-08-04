package bg.martinandonov.restaurant.diningtable.controller;

import java.util.List;
import java.util.Locale;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import bg.martinandonov.restaurant.common.exception.InvalidRequestException;
import bg.martinandonov.restaurant.diningtable.dto.DiningTableResponse;
import bg.martinandonov.restaurant.diningtable.dto.UpdateDiningTableStatusRequest;
import bg.martinandonov.restaurant.diningtable.entity.DiningTableStatus;
import bg.martinandonov.restaurant.diningtable.service.DiningTableService;

@RestController
@RequestMapping("/api/waiter/tables")
public class WaiterDiningTableController {

	private final DiningTableService diningTableService;

	public WaiterDiningTableController(DiningTableService diningTableService) {
		this.diningTableService = diningTableService;
	}

	@GetMapping
	public ResponseEntity<List<DiningTableResponse>> getActiveTables(
			@RequestParam(required = false) String status) {
		if (status == null || status.isBlank()) {
			return ResponseEntity.ok(diningTableService.getActiveTables());
		}
		return ResponseEntity.ok(diningTableService.getActiveTablesByStatus(parseStatus(status)));
	}

	@GetMapping("/{id}")
	public ResponseEntity<DiningTableResponse> getActiveTableById(@PathVariable Long id) {
		return ResponseEntity.ok(diningTableService.getActiveTableById(id));
	}

	@PatchMapping("/{id}/status")
	public ResponseEntity<DiningTableResponse> updateStatus(
			@PathVariable Long id,
			@RequestBody UpdateDiningTableStatusRequest request) {
		return ResponseEntity.ok(diningTableService.updateActiveTableStatus(id, request));
	}

	private DiningTableStatus parseStatus(String status) {
		try {
			return DiningTableStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
		}
		catch (IllegalArgumentException ex) {
			throw new InvalidRequestException("Unknown dining table status: " + status.trim());
		}
	}
}
