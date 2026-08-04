package bg.martinandonov.restaurant.inventory.controller;

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

import bg.martinandonov.restaurant.inventory.dto.AdjustIngredientStockRequest;
import bg.martinandonov.restaurant.inventory.dto.CreateIngredientRequest;
import bg.martinandonov.restaurant.inventory.dto.IngredientResponse;
import bg.martinandonov.restaurant.inventory.dto.UpdateIngredientRequest;
import bg.martinandonov.restaurant.inventory.dto.UpdateIngredientStatusRequest;
import bg.martinandonov.restaurant.inventory.service.IngredientService;

@RestController
@RequestMapping("/api/admin/inventory/ingredients")
public class AdminIngredientController {

	private final IngredientService ingredientService;

	public AdminIngredientController(IngredientService ingredientService) {
		this.ingredientService = ingredientService;
	}

	@PostMapping
	public ResponseEntity<IngredientResponse> createIngredient(@RequestBody CreateIngredientRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(ingredientService.createIngredient(request));
	}

	@GetMapping
	public ResponseEntity<List<IngredientResponse>> getIngredients(
			@RequestParam(required = false, defaultValue = "false") boolean activeOnly) {
		if (activeOnly) {
			return ResponseEntity.ok(ingredientService.getActiveIngredients());
		}
		return ResponseEntity.ok(ingredientService.getAllIngredients());
	}

	@GetMapping("/{id}")
	public ResponseEntity<IngredientResponse> getIngredientById(@PathVariable Long id) {
		return ResponseEntity.ok(ingredientService.getIngredientById(id));
	}

	@PutMapping("/{id}")
	public ResponseEntity<IngredientResponse> updateIngredient(
			@PathVariable Long id,
			@RequestBody UpdateIngredientRequest request) {
		return ResponseEntity.ok(ingredientService.updateIngredient(id, request));
	}

	@PatchMapping("/{id}/status")
	public ResponseEntity<IngredientResponse> updateIngredientStatus(
			@PathVariable Long id,
			@RequestBody UpdateIngredientStatusRequest request) {
		return ResponseEntity.ok(ingredientService.updateIngredientStatus(id, request));
	}

	@PatchMapping("/{id}/stock")
	public ResponseEntity<IngredientResponse> adjustStock(
			@PathVariable Long id,
			@RequestBody AdjustIngredientStockRequest request) {
		return ResponseEntity.ok(ingredientService.adjustStock(id, request));
	}
}
