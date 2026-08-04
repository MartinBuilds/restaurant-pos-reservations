package bg.martinandonov.restaurant.menu.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import bg.martinandonov.restaurant.menu.dto.MenuCategoryResponse;
import bg.martinandonov.restaurant.menu.dto.MenuItemResponse;
import bg.martinandonov.restaurant.menu.service.MenuCategoryService;
import bg.martinandonov.restaurant.menu.service.MenuItemService;

@RestController
@RequestMapping("/api/public/menu")
public class PublicMenuController {

	private final MenuCategoryService menuCategoryService;
	private final MenuItemService menuItemService;

	public PublicMenuController(MenuCategoryService menuCategoryService, MenuItemService menuItemService) {
		this.menuCategoryService = menuCategoryService;
		this.menuItemService = menuItemService;
	}

	@GetMapping
	public ResponseEntity<List<MenuItemResponse>> getPublicMenu() {
		return ResponseEntity.ok(menuItemService.getPublicMenu());
	}

	@GetMapping("/categories")
	public ResponseEntity<List<MenuCategoryResponse>> getPublicCategories() {
		return ResponseEntity.ok(menuCategoryService.getActiveCategories());
	}
}
