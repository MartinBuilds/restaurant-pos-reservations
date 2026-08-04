package bg.martinandonov.restaurant.inventory.service;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import bg.martinandonov.restaurant.common.exception.InvalidRequestException;
import bg.martinandonov.restaurant.common.exception.ResourceNotFoundException;
import bg.martinandonov.restaurant.inventory.dto.RecipeComponentRequest;
import bg.martinandonov.restaurant.inventory.dto.RecipeComponentResponse;
import bg.martinandonov.restaurant.inventory.dto.RecipeResponse;
import bg.martinandonov.restaurant.inventory.dto.ReplaceRecipeRequest;
import bg.martinandonov.restaurant.inventory.entity.Ingredient;
import bg.martinandonov.restaurant.inventory.entity.RecipeIngredient;
import bg.martinandonov.restaurant.inventory.repository.RecipeIngredientRepository;
import bg.martinandonov.restaurant.menu.entity.MenuItem;
import bg.martinandonov.restaurant.menu.repository.MenuItemRepository;
import bg.martinandonov.restaurant.menu.service.MenuAvailabilityService;

@Service
@Transactional
public class RecipeService {

	private final RecipeIngredientRepository recipeIngredientRepository;
	private final MenuItemRepository menuItemRepository;
	private final IngredientService ingredientService;
	private final MenuAvailabilityService menuAvailabilityService;

	public RecipeService(
			RecipeIngredientRepository recipeIngredientRepository,
			MenuItemRepository menuItemRepository,
			IngredientService ingredientService,
			MenuAvailabilityService menuAvailabilityService) {
		this.recipeIngredientRepository = recipeIngredientRepository;
		this.menuItemRepository = menuItemRepository;
		this.ingredientService = ingredientService;
		this.menuAvailabilityService = menuAvailabilityService;
	}

	@Transactional(readOnly = true)
	public RecipeResponse getRecipeForMenuItem(Long menuItemId) {
		MenuItem menuItem = findMenuItem(menuItemId);
		List<RecipeIngredient> components =
				recipeIngredientRepository.findByMenuItemIdOrderByIngredientNameAsc(menuItemId);
		return toResponse(menuItem, components);
	}

	public RecipeResponse replaceRecipeForMenuItem(Long menuItemId, ReplaceRecipeRequest request) {
		Objects.requireNonNull(request, "request must not be null");
		MenuItem menuItem = findMenuItem(menuItemId);
		List<RecipeComponentRequest> components = request.getComponents();
		if (components == null || components.isEmpty()) {
			throw new InvalidRequestException("Recipe must contain at least one ingredient");
		}

		Set<Long> seenIngredientIds = new HashSet<>();
		for (RecipeComponentRequest component : components) {
			if (component == null || component.getIngredientId() == null) {
				throw new InvalidRequestException("ingredientId must be provided");
			}
			if (!seenIngredientIds.add(component.getIngredientId())) {
				throw new InvalidRequestException("Duplicate ingredient ids are not allowed in a recipe");
			}
			requirePositiveQuantity(component.getQuantityRequired());
		}

		List<RecipeIngredient> replacements = components.stream()
				.map(component -> {
					Ingredient ingredient = ingredientService.getActiveIngredientEntity(component.getIngredientId());
					return new RecipeIngredient(menuItem, ingredient, component.getQuantityRequired());
				})
				.toList();

		recipeIngredientRepository.deleteByMenuItemId(menuItemId);
		recipeIngredientRepository.flush();
		List<RecipeIngredient> saved = recipeIngredientRepository.saveAll(replacements);
		menuAvailabilityService.recalculateAvailability(menuItemId);
		return toResponse(menuItem, saved);
	}

	public void removeRecipeForMenuItem(Long menuItemId) {
		findMenuItem(menuItemId);
		recipeIngredientRepository.deleteByMenuItemId(menuItemId);
		menuAvailabilityService.recalculateAvailability(menuItemId);
	}

	private MenuItem findMenuItem(Long menuItemId) {
		if (menuItemId == null) {
			throw new InvalidRequestException("Menu item id must be provided");
		}
		return menuItemRepository.findById(menuItemId)
				.orElseThrow(() -> new ResourceNotFoundException("Menu item not found: " + menuItemId));
	}

	private BigDecimal requirePositiveQuantity(BigDecimal quantityRequired) {
		if (quantityRequired == null) {
			throw new InvalidRequestException("quantityRequired must be provided");
		}
		if (quantityRequired.compareTo(BigDecimal.ZERO) <= 0) {
			throw new InvalidRequestException("quantityRequired must be greater than 0");
		}
		return quantityRequired;
	}

	private RecipeResponse toResponse(MenuItem menuItem, List<RecipeIngredient> components) {
		List<RecipeComponentResponse> componentResponses = components.stream()
				.map(component -> new RecipeComponentResponse(
						component.getIngredient().getId(),
						component.getIngredient().getName(),
						component.getIngredient().getUnit().name(),
						component.getQuantityRequired()))
				.toList();
		return new RecipeResponse(menuItem.getId(), menuItem.getName(), componentResponses);
	}
}
