package bg.martinandonov.restaurant.diningtable.controller;

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
import org.springframework.web.bind.annotation.RestController;

import bg.martinandonov.restaurant.diningtable.dto.CreateDiningTableRequest;
import bg.martinandonov.restaurant.diningtable.dto.DiningTableResponse;
import bg.martinandonov.restaurant.diningtable.dto.UpdateDiningTableActiveRequest;
import bg.martinandonov.restaurant.diningtable.dto.UpdateDiningTableRequest;
import bg.martinandonov.restaurant.diningtable.dto.UpdateDiningTableStatusRequest;
import bg.martinandonov.restaurant.diningtable.service.DiningTableService;

@RestController
@RequestMapping("/api/admin/tables")
public class AdminDiningTableController {

	private final DiningTableService diningTableService;

	public AdminDiningTableController(DiningTableService diningTableService) {
		this.diningTableService = diningTableService;
	}

	@PostMapping
	public ResponseEntity<DiningTableResponse> createTable(@RequestBody CreateDiningTableRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(diningTableService.createTable(request));
	}

	@GetMapping
	public ResponseEntity<List<DiningTableResponse>> getAllTables() {
		return ResponseEntity.ok(diningTableService.getAllTables());
	}

	@GetMapping("/{id}")
	public ResponseEntity<DiningTableResponse> getTableById(@PathVariable Long id) {
		return ResponseEntity.ok(diningTableService.getTableById(id));
	}

	@PutMapping("/{id}")
	public ResponseEntity<DiningTableResponse> updateTable(
			@PathVariable Long id,
			@RequestBody UpdateDiningTableRequest request) {
		return ResponseEntity.ok(diningTableService.updateTable(id, request));
	}

	@PatchMapping("/{id}/status")
	public ResponseEntity<DiningTableResponse> updateStatus(
			@PathVariable Long id,
			@RequestBody UpdateDiningTableStatusRequest request) {
		return ResponseEntity.ok(diningTableService.updateTableStatus(id, request));
	}

	@PatchMapping("/{id}/active")
	public ResponseEntity<DiningTableResponse> updateActive(
			@PathVariable Long id,
			@RequestBody UpdateDiningTableActiveRequest request) {
		return ResponseEntity.ok(diningTableService.updateTableActiveState(id, request));
	}
}
