package bg.martinandonov.restaurant.menu.service;

import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import bg.martinandonov.restaurant.common.exception.BusinessRuleException;
import bg.martinandonov.restaurant.common.exception.InvalidRequestException;
import bg.martinandonov.restaurant.common.exception.ResourceNotFoundException;
import bg.martinandonov.restaurant.menu.dto.CreateMenuCategoryRequest;
import bg.martinandonov.restaurant.menu.dto.MenuCategoryResponse;
import bg.martinandonov.restaurant.menu.dto.UpdateMenuCategoryRequest;
import bg.martinandonov.restaurant.menu.dto.UpdateMenuCategoryStatusRequest;
import bg.martinandonov.restaurant.menu.entity.MenuCategory;
import bg.martinandonov.restaurant.menu.repository.MenuCategoryRepository;

@Service
@Transactional
public class MenuCategoryService {

	private static final int MAX_NAME_LENGTH = 100;
	private static final int MAX_DESCRIPTION_LENGTH = 500;

	private final MenuCategoryRepository menuCategoryRepository;

	public MenuCategoryService(MenuCategoryRepository menuCategoryRepository) {
		this.menuCategoryRepository = menuCategoryRepository;
	}

	public MenuCategoryResponse createCategory(CreateMenuCategoryRequest request) {
		Objects.requireNonNull(request, "request must not be null");
		String name = requireName(request.getName());
		String description = normalizeDescription(request.getDescription());

		if (menuCategoryRepository.existsByNameIgnoreCase(name)) {
			throw new BusinessRuleException("A menu category with this name already exists");
		}

		MenuCategory category = new MenuCategory(name, description, true);
		return toResponse(menuCategoryRepository.save(category));
	}

	@Transactional(readOnly = true)
	public List<MenuCategoryResponse> getAllCategories() {
		return menuCategoryRepository.findAllByOrderByNameAsc().stream()
				.map(this::toResponse)
				.toList();
	}

	@Transactional(readOnly = true)
	public List<MenuCategoryResponse> getActiveCategories() {
		return menuCategoryRepository.findByActiveTrueOrderByNameAsc().stream()
				.map(this::toResponse)
				.toList();
	}

	@Transactional(readOnly = true)
	public MenuCategoryResponse getCategoryById(Long id) {
		return toResponse(findCategory(id));
	}

	public MenuCategoryResponse updateCategory(Long id, UpdateMenuCategoryRequest request) {
		Objects.requireNonNull(request, "request must not be null");
		MenuCategory category = findCategory(id);
		String name = requireName(request.getName());
		String description = normalizeDescription(request.getDescription());

		if (menuCategoryRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
			throw new BusinessRuleException("A menu category with this name already exists");
		}

		category.setName(name);
		category.setDescription(description);
		return toResponse(category);
	}

	public MenuCategoryResponse updateCategoryStatus(Long id, UpdateMenuCategoryStatusRequest request) {
		Objects.requireNonNull(request, "request must not be null");
		if (request.getActive() == null) {
			throw new InvalidRequestException("active must be provided");
		}
		MenuCategory category = findCategory(id);
		category.setActive(request.getActive());
		return toResponse(category);
	}

	@Transactional(readOnly = true)
	public MenuCategory getCategoryEntity(Long id) {
		return findCategory(id);
	}

	private MenuCategory findCategory(Long id) {
		if (id == null) {
			throw new InvalidRequestException("Category id must be provided");
		}
		return menuCategoryRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Menu category not found: " + id));
	}

	private String requireName(String name) {
		if (name == null || name.isBlank()) {
			throw new InvalidRequestException("Category name must not be blank");
		}
		String trimmed = name.trim();
		if (trimmed.length() > MAX_NAME_LENGTH) {
			throw new InvalidRequestException("Category name must be at most " + MAX_NAME_LENGTH + " characters");
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
					"Category description must be at most " + MAX_DESCRIPTION_LENGTH + " characters");
		}
		return trimmed;
	}

	private MenuCategoryResponse toResponse(MenuCategory category) {
		return new MenuCategoryResponse(
				category.getId(),
				category.getName(),
				category.getDescription(),
				category.isActive());
	}
}
