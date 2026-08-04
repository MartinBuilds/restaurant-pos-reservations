package bg.martinandonov.restaurant.menu.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import bg.martinandonov.restaurant.common.exception.InvalidRequestException;
import bg.martinandonov.restaurant.common.exception.ResourceNotFoundException;
import bg.martinandonov.restaurant.inventory.entity.RecipeIngredient;
import bg.martinandonov.restaurant.inventory.repository.RecipeIngredientRepository;
import bg.martinandonov.restaurant.menu.dto.MenuAvailabilityResponse;
import bg.martinandonov.restaurant.menu.entity.MenuAvailabilityReason;
import bg.martinandonov.restaurant.menu.entity.MenuItem;
import bg.martinandonov.restaurant.menu.repository.MenuItemRepository;

@Service
@Transactional
public class MenuAvailabilityService {

	private final MenuItemRepository menuItemRepository;
	private final RecipeIngredientRepository recipeIngredientRepository;

	public MenuAvailabilityService(
			MenuItemRepository menuItemRepository,
			RecipeIngredientRepository recipeIngredientRepository) {
		this.menuItemRepository = menuItemRepository;
		this.recipeIngredientRepository = recipeIngredientRepository;
	}

	@Transactional(readOnly = true)
	public AvailabilityEvaluation evaluateAvailability(Long menuItemId) {
		MenuItem item = findMenuItem(menuItemId);
		List<RecipeIngredient> components =
				recipeIngredientRepository.findByMenuItemIdOrderByIngredientNameAsc(menuItemId);
		return evaluate(item, components);
	}

	public MenuAvailabilityResponse recalculateAvailability(Long menuItemId) {
		MenuItem item = findMenuItem(menuItemId);
		List<RecipeIngredient> components =
				recipeIngredientRepository.findByMenuItemIdOrderByIngredientNameAsc(menuItemId);
		AvailabilityEvaluation evaluation = evaluate(item, components);
		applyIfChanged(item, evaluation);
		return toResponse(item, evaluation.maxPossibleServings());
	}

	public List<MenuAvailabilityResponse> recalculateForIngredient(Long ingredientId) {
		if (ingredientId == null) {
			throw new InvalidRequestException("Ingredient id must be provided");
		}
		List<Long> menuItemIds =
				recipeIngredientRepository.findDistinctMenuItemIdsByIngredientIdOrderByMenuItemIdAsc(ingredientId);
		List<MenuAvailabilityResponse> responses = new ArrayList<>(menuItemIds.size());
		for (Long menuItemId : menuItemIds) {
			responses.add(recalculateAvailability(menuItemId));
		}
		return responses;
	}

	public List<MenuAvailabilityResponse> recalculateAllMenuItems() {
		List<MenuItem> items = menuItemRepository.findAllByOrderByIdAsc();
		List<MenuAvailabilityResponse> responses = new ArrayList<>(items.size());
		for (MenuItem item : items) {
			responses.add(recalculateAvailability(item.getId()));
		}
		return responses;
	}

	@Transactional(readOnly = true)
	public MenuAvailabilityResponse getAvailability(Long menuItemId) {
		MenuItem item = findMenuItem(menuItemId);
		List<RecipeIngredient> components =
				recipeIngredientRepository.findByMenuItemIdOrderByIngredientNameAsc(menuItemId);
		AvailabilityEvaluation evaluation = evaluate(item, components);
		return new MenuAvailabilityResponse(
				item.getId(),
				item.getName(),
				item.isManualAvailable(),
				evaluation.available(),
				evaluation.reason().name(),
				evaluation.maxPossibleServings());
	}

	AvailabilityEvaluation evaluate(MenuItem item, List<RecipeIngredient> components) {
		Objects.requireNonNull(item, "item must not be null");
		List<RecipeIngredient> recipe = components == null ? List.of() : components;

		long maxPossibleServings = computeMaxPossibleServings(recipe);

		if (!item.isManualAvailable()) {
			return new AvailabilityEvaluation(false, MenuAvailabilityReason.MANUALLY_DISABLED, maxPossibleServings);
		}
		if (recipe.isEmpty()) {
			return new AvailabilityEvaluation(false, MenuAvailabilityReason.NO_RECIPE, 0L);
		}
		if (hasInactiveIngredient(recipe)) {
			return new AvailabilityEvaluation(false, MenuAvailabilityReason.INACTIVE_INGREDIENT, 0L);
		}
		if (hasInsufficientStock(recipe)) {
			return new AvailabilityEvaluation(false, MenuAvailabilityReason.INSUFFICIENT_STOCK, maxPossibleServings);
		}
		return new AvailabilityEvaluation(true, MenuAvailabilityReason.AVAILABLE, maxPossibleServings);
	}

	private long computeMaxPossibleServings(List<RecipeIngredient> recipe) {
		if (recipe.isEmpty()) {
			return 0L;
		}
		if (hasInactiveIngredient(recipe)) {
			return 0L;
		}
		Long minServings = null;
		for (RecipeIngredient component : recipe) {
			BigDecimal stock = component.getIngredient().getStockQuantity();
			BigDecimal required = component.getQuantityRequired();
			long servings = stock.divide(required, 0, RoundingMode.DOWN).longValueExact();
			if (minServings == null || servings < minServings) {
				minServings = servings;
			}
		}
		return minServings == null ? 0L : Math.max(minServings, 0L);
	}

	private boolean hasInactiveIngredient(List<RecipeIngredient> recipe) {
		return recipe.stream().anyMatch(component -> !component.getIngredient().isActive());
	}

	private boolean hasInsufficientStock(List<RecipeIngredient> recipe) {
		return recipe.stream().anyMatch(component ->
				component.getIngredient().getStockQuantity().compareTo(component.getQuantityRequired()) < 0);
	}

	private void applyIfChanged(MenuItem item, AvailabilityEvaluation evaluation) {
		if (item.isAvailable() != evaluation.available()
				|| item.getAvailabilityReason() != evaluation.reason()) {
			item.setAvailable(evaluation.available());
			item.setAvailabilityReason(evaluation.reason());
		}
	}

	private MenuItem findMenuItem(Long menuItemId) {
		if (menuItemId == null) {
			throw new InvalidRequestException("Menu item id must be provided");
		}
		return menuItemRepository.findById(menuItemId)
				.orElseThrow(() -> new ResourceNotFoundException("Menu item not found: " + menuItemId));
	}

	private MenuAvailabilityResponse toResponse(MenuItem item, long maxPossibleServings) {
		return new MenuAvailabilityResponse(
				item.getId(),
				item.getName(),
				item.isManualAvailable(),
				item.isAvailable(),
				item.getAvailabilityReason().name(),
				maxPossibleServings);
	}

	public record AvailabilityEvaluation(
			boolean available,
			MenuAvailabilityReason reason,
			long maxPossibleServings) {
	}
}
