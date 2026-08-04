package bg.martinandonov.restaurant.menu.controller;

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

import bg.martinandonov.restaurant.menu.dto.CreateMenuCategoryRequest;
import bg.martinandonov.restaurant.menu.dto.MenuCategoryResponse;
import bg.martinandonov.restaurant.menu.dto.UpdateMenuCategoryRequest;
import bg.martinandonov.restaurant.menu.dto.UpdateMenuCategoryStatusRequest;
import bg.martinandonov.restaurant.menu.service.MenuCategoryService;

@RestController
@RequestMapping("/api/admin/menu/categories")
public class AdminMenuCategoryController {

	private final MenuCategoryService menuCategoryService;

	public AdminMenuCategoryController(MenuCategoryService menuCategoryService) {
		this.menuCategoryService = menuCategoryService;
	}

	@PostMapping
	public ResponseEntity<MenuCategoryResponse> createCategory(@RequestBody CreateMenuCategoryRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(menuCategoryService.createCategory(request));
	}

	@GetMapping
	public ResponseEntity<List<MenuCategoryResponse>> getAllCategories() {
		return ResponseEntity.ok(menuCategoryService.getAllCategories());
	}

	@GetMapping("/{id}")
	public ResponseEntity<MenuCategoryResponse> getCategoryById(@PathVariable Long id) {
		return ResponseEntity.ok(menuCategoryService.getCategoryById(id));
	}

	@PutMapping("/{id}")
	public ResponseEntity<MenuCategoryResponse> updateCategory(
			@PathVariable Long id,
			@RequestBody UpdateMenuCategoryRequest request) {
		return ResponseEntity.ok(menuCategoryService.updateCategory(id, request));
	}

	@PatchMapping("/{id}/status")
	public ResponseEntity<MenuCategoryResponse> updateCategoryStatus(
			@PathVariable Long id,
			@RequestBody UpdateMenuCategoryStatusRequest request) {
		return ResponseEntity.ok(menuCategoryService.updateCategoryStatus(id, request));
	}
}
