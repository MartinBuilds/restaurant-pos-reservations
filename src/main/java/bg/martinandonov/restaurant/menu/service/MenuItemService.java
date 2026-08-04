package bg.martinandonov.restaurant.menu.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import bg.martinandonov.restaurant.common.exception.BusinessRuleException;
import bg.martinandonov.restaurant.common.exception.InvalidRequestException;
import bg.martinandonov.restaurant.common.exception.ResourceNotFoundException;
import bg.martinandonov.restaurant.menu.dto.CreateMenuItemRequest;
import bg.martinandonov.restaurant.menu.dto.MenuItemResponse;
import bg.martinandonov.restaurant.menu.dto.UpdateMenuItemAvailabilityRequest;
import bg.martinandonov.restaurant.menu.dto.UpdateMenuItemRequest;
import bg.martinandonov.restaurant.menu.dto.UpdateMenuItemStatusRequest;
import bg.martinandonov.restaurant.menu.entity.MenuCategory;
import bg.martinandonov.restaurant.menu.entity.MenuItem;
import bg.martinandonov.restaurant.menu.repository.MenuItemRepository;

@Service
@Transactional
public class MenuItemService {

	private static final int MAX_NAME_LENGTH = 150;
	private static final int MAX_DESCRIPTION_LENGTH = 1000;

	private final MenuItemRepository menuItemRepository;
	private final MenuCategoryService menuCategoryService;

	public MenuItemService(MenuItemRepository menuItemRepository, MenuCategoryService menuCategoryService) {
		this.menuItemRepository = menuItemRepository;
		this.menuCategoryService = menuCategoryService;
	}

	public MenuItemResponse createMenuItem(CreateMenuItemRequest request) {
		Objects.requireNonNull(request, "request must not be null");
		String name = requireName(request.getName());
		String description = normalizeDescription(request.getDescription());
		BigDecimal price = requirePositivePrice(request.getPrice());
		MenuCategory category = menuCategoryService.getCategoryEntity(request.getCategoryId());
		boolean available = request.getAvailable() == null || request.getAvailable();

		ensureCategoryAllowsActiveItem(category);
		ensureUniqueItemName(category.getId(), name, null);

		MenuItem item = new MenuItem(name, description, price, true, available, category);
		return toResponse(menuItemRepository.save(item));
	}

	@Transactional(readOnly = true)
	public List<MenuItemResponse> getAllMenuItems() {
		return menuItemRepository.findAllByOrderByIdAsc().stream()
				.map(this::toResponse)
				.toList();
	}

	@Transactional(readOnly = true)
	public MenuItemResponse getMenuItemById(Long id) {
		return toResponse(findItem(id));
	}

	@Transactional(readOnly = true)
	public List<MenuItemResponse> getMenuItemsByCategory(Long categoryId) {
		menuCategoryService.getCategoryEntity(categoryId);
		return menuItemRepository.findByCategoryIdOrderByNameAsc(categoryId).stream()
				.map(this::toResponse)
				.toList();
	}

	@Transactional(readOnly = true)
	public List<MenuItemResponse> getPublicMenu() {
		return menuItemRepository.findByActiveTrueAndAvailableTrueAndCategory_ActiveTrueOrderByNameAsc().stream()
				.map(this::toResponse)
				.toList();
	}

	public MenuItemResponse updateMenuItem(Long id, UpdateMenuItemRequest request) {
		Objects.requireNonNull(request, "request must not be null");
		MenuItem item = findItem(id);
		String name = requireName(request.getName());
		String description = normalizeDescription(request.getDescription());
		BigDecimal price = requirePositivePrice(request.getPrice());
		MenuCategory category = menuCategoryService.getCategoryEntity(request.getCategoryId());
		boolean available = request.getAvailable() == null ? item.isAvailable() : request.getAvailable();

		if (item.isActive()) {
			ensureCategoryAllowsActiveItem(category);
		}
		ensureUniqueItemName(category.getId(), name, id);

		item.setName(name);
		item.setDescription(description);
		item.setPrice(price);
		item.setCategory(category);
		item.setAvailable(available);
		return toResponse(item);
	}

	public MenuItemResponse updateMenuItemStatus(Long id, UpdateMenuItemStatusRequest request) {
		Objects.requireNonNull(request, "request must not be null");
		if (request.getActive() == null) {
			throw new InvalidRequestException("active must be provided");
		}
		MenuItem item = findItem(id);
		if (request.getActive()) {
			ensureCategoryAllowsActiveItem(item.getCategory());
		}
		item.setActive(request.getActive());
		return toResponse(item);
	}

	public MenuItemResponse updateAvailability(Long id, UpdateMenuItemAvailabilityRequest request) {
		Objects.requireNonNull(request, "request must not be null");
		if (request.getAvailable() == null) {
			throw new InvalidRequestException("available must be provided");
		}
		MenuItem item = findItem(id);
		item.setAvailable(request.getAvailable());
		return toResponse(item);
	}

	private MenuItem findItem(Long id) {
		if (id == null) {
			throw new InvalidRequestException("Menu item id must be provided");
		}
		return menuItemRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Menu item not found: " + id));
	}

	private void ensureCategoryAllowsActiveItem(MenuCategory category) {
		if (!category.isActive()) {
			throw new BusinessRuleException("Cannot assign an active menu item to an inactive category");
		}
	}

	private void ensureUniqueItemName(Long categoryId, String name, Long currentItemId) {
		boolean duplicate = currentItemId == null
				? menuItemRepository.existsByCategoryIdAndNameIgnoreCase(categoryId, name)
				: menuItemRepository.existsByCategoryIdAndNameIgnoreCaseAndIdNot(categoryId, name, currentItemId);
		if (duplicate) {
			throw new BusinessRuleException("A menu item with this name already exists in the category");
		}
	}

	private String requireName(String name) {
		if (name == null || name.isBlank()) {
			throw new InvalidRequestException("Menu item name must not be blank");
		}
		String trimmed = name.trim();
		if (trimmed.length() > MAX_NAME_LENGTH) {
			throw new InvalidRequestException("Menu item name must be at most " + MAX_NAME_LENGTH + " characters");
		}
		return trimmed;
	}

	private String normalizeDescription(String description) {
		if (description == null || description.isBlank()) {
			return null;
		}
		String trimmed = description.trim();
		if (trimmed.length() > MAX_DESCRIPTION_LENGTH) {
			throw new InvalidRequestException(
					"Menu item description must be at most " + MAX_DESCRIPTION_LENGTH + " characters");
		}
		return trimmed;
	}

	private BigDecimal requirePositivePrice(BigDecimal price) {
		if (price == null) {
			throw new InvalidRequestException("Price must be provided");
		}
		if (price.compareTo(BigDecimal.ZERO) <= 0) {
			throw new InvalidRequestException("Price must be greater than 0");
		}
		return price;
	}

	private MenuItemResponse toResponse(MenuItem item) {
		MenuCategory category = item.getCategory();
		return new MenuItemResponse(
				item.getId(),
				item.getName(),
				item.getDescription(),
				item.getPrice(),
				item.isActive(),
				item.isAvailable(),
				category.getId(),
				category.getName());
	}
}
