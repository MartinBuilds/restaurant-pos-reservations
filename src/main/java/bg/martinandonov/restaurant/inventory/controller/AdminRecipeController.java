package bg.martinandonov.restaurant.inventory.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import bg.martinandonov.restaurant.inventory.dto.RecipeResponse;
import bg.martinandonov.restaurant.inventory.dto.ReplaceRecipeRequest;
import bg.martinandonov.restaurant.inventory.service.RecipeService;

@RestController
@RequestMapping("/api/admin/menu/items/{menuItemId}/recipe")
public class AdminRecipeController {

	private final RecipeService recipeService;

	public AdminRecipeController(RecipeService recipeService) {
		this.recipeService = recipeService;
	}

	@GetMapping
	public ResponseEntity<RecipeResponse> getRecipe(@PathVariable Long menuItemId) {
		return ResponseEntity.ok(recipeService.getRecipeForMenuItem(menuItemId));
	}

	@PutMapping
	public ResponseEntity<RecipeResponse> replaceRecipe(
			@PathVariable Long menuItemId,
			@RequestBody ReplaceRecipeRequest request) {
		return ResponseEntity.ok(recipeService.replaceRecipeForMenuItem(menuItemId, request));
	}

	@DeleteMapping
	public ResponseEntity<Void> removeRecipe(@PathVariable Long menuItemId) {
		recipeService.removeRecipeForMenuItem(menuItemId);
		return ResponseEntity.noContent().build();
	}
}
