package bg.martinandonov.restaurant.menu.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import bg.martinandonov.restaurant.menu.dto.MenuAvailabilityResponse;
import bg.martinandonov.restaurant.menu.service.MenuItemService;

@RestController
@RequestMapping("/api/admin/menu/availability")
public class AdminMenuAvailabilityController {

	private final MenuItemService menuItemService;

	public AdminMenuAvailabilityController(MenuItemService menuItemService) {
		this.menuItemService = menuItemService;
	}

	@PostMapping("/recalculate")
	public ResponseEntity<List<MenuAvailabilityResponse>> recalculateAll() {
		return ResponseEntity.ok(menuItemService.recalculateAllAvailability());
	}
}
