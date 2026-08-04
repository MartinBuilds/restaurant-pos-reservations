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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import bg.martinandonov.restaurant.menu.dto.CreateMenuItemRequest;
import bg.martinandonov.restaurant.menu.dto.MenuItemResponse;
import bg.martinandonov.restaurant.menu.dto.UpdateMenuItemAvailabilityRequest;
import bg.martinandonov.restaurant.menu.dto.UpdateMenuItemRequest;
import bg.martinandonov.restaurant.menu.dto.UpdateMenuItemStatusRequest;
import bg.martinandonov.restaurant.menu.service.MenuItemService;

@RestController
@RequestMapping("/api/admin/menu/items")
public class AdminMenuItemController {

	private final MenuItemService menuItemService;

	public AdminMenuItemController(MenuItemService menuItemService) {
		this.menuItemService = menuItemService;
	}

	@PostMapping
	public ResponseEntity<MenuItemResponse> createMenuItem(@RequestBody CreateMenuItemRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(menuItemService.createMenuItem(request));
	}

	@GetMapping
	public ResponseEntity<List<MenuItemResponse>> getMenuItems(
			@RequestParam(required = false) Long categoryId) {
		if (categoryId != null) {
			return ResponseEntity.ok(menuItemService.getMenuItemsByCategory(categoryId));
		}
		return ResponseEntity.ok(menuItemService.getAllMenuItems());
	}

	@GetMapping("/{id}")
	public ResponseEntity<MenuItemResponse> getMenuItemById(@PathVariable Long id) {
		return ResponseEntity.ok(menuItemService.getMenuItemById(id));
	}

	@PutMapping("/{id}")
	public ResponseEntity<MenuItemResponse> updateMenuItem(
			@PathVariable Long id,
			@RequestBody UpdateMenuItemRequest request) {
		return ResponseEntity.ok(menuItemService.updateMenuItem(id, request));
	}

	@PatchMapping("/{id}/status")
	public ResponseEntity<MenuItemResponse> updateMenuItemStatus(
			@PathVariable Long id,
			@RequestBody UpdateMenuItemStatusRequest request) {
		return ResponseEntity.ok(menuItemService.updateMenuItemStatus(id, request));
	}

	@PatchMapping("/{id}/availability")
	public ResponseEntity<MenuItemResponse> updateAvailability(
			@PathVariable Long id,
			@RequestBody UpdateMenuItemAvailabilityRequest request) {
		return ResponseEntity.ok(menuItemService.updateAvailability(id, request));
	}
}
