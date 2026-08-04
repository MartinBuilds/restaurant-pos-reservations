package bg.martinandonov.restaurant.menu.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import bg.martinandonov.restaurant.common.exception.ResourceNotFoundException;
import bg.martinandonov.restaurant.inventory.entity.Ingredient;
import bg.martinandonov.restaurant.inventory.entity.IngredientUnit;
import bg.martinandonov.restaurant.inventory.entity.RecipeIngredient;
import bg.martinandonov.restaurant.inventory.repository.RecipeIngredientRepository;
import bg.martinandonov.restaurant.menu.dto.MenuAvailabilityResponse;
import bg.martinandonov.restaurant.menu.entity.MenuAvailabilityReason;
import bg.martinandonov.restaurant.menu.entity.MenuCategory;
import bg.martinandonov.restaurant.menu.entity.MenuItem;
import bg.martinandonov.restaurant.menu.repository.MenuItemRepository;

@ExtendWith(MockitoExtension.class)
class MenuAvailabilityServiceTest {

	@Mock
	private MenuItemRepository menuItemRepository;

	@Mock
	private RecipeIngredientRepository recipeIngredientRepository;

	@InjectMocks
	private MenuAvailabilityService menuAvailabilityService;

	private MenuCategory category;
	private MenuItem item;

	@BeforeEach
	void setUp() {
		category = new MenuCategory("Salads", null, true);
		ReflectionTestUtils.setField(category, "id", 1L);
		item = new MenuItem("Caesar Salad", null, new BigDecimal("12.50"), true, true, category);
		ReflectionTestUtils.setField(item, "id", 10L);
	}

	@Test
	void itemWithoutRecipeIsUnavailableWithNoRecipe() {
		when(menuItemRepository.findById(10L)).thenReturn(Optional.of(item));
		when(recipeIngredientRepository.findByMenuItemIdOrderByIngredientNameAsc(10L)).thenReturn(List.of());

		MenuAvailabilityResponse response = menuAvailabilityService.recalculateAvailability(10L);

		assertThat(item.isAvailable()).isFalse();
		assertThat(item.getAvailabilityReason()).isEqualTo(MenuAvailabilityReason.NO_RECIPE);
		assertThat(response.getMaxPossibleServings()).isZero();
		assertThat(response.getAvailabilityReason()).isEqualTo("NO_RECIPE");
	}

	@Test
	void manualAvailableFalseYieldsManuallyDisabled() {
		item.setManualAvailable(false);
		Ingredient tomato = ingredient(1L, "Tomato", "1000", true);
		when(menuItemRepository.findById(10L)).thenReturn(Optional.of(item));
		when(recipeIngredientRepository.findByMenuItemIdOrderByIngredientNameAsc(10L))
				.thenReturn(List.of(component(tomato, "100")));

		MenuAvailabilityResponse response = menuAvailabilityService.recalculateAvailability(10L);

		assertThat(response.isAvailable()).isFalse();
		assertThat(response.getAvailabilityReason()).isEqualTo("MANUALLY_DISABLED");
		assertThat(response.getMaxPossibleServings()).isEqualTo(10L);
	}

	@Test
	void sufficientStockYieldsAvailable() {
		Ingredient tomato = ingredient(1L, "Tomato", "1000", true);
		when(menuItemRepository.findById(10L)).thenReturn(Optional.of(item));
		when(recipeIngredientRepository.findByMenuItemIdOrderByIngredientNameAsc(10L))
				.thenReturn(List.of(component(tomato, "250")));

		MenuAvailabilityResponse response = menuAvailabilityService.recalculateAvailability(10L);

		assertThat(response.isAvailable()).isTrue();
		assertThat(response.getAvailabilityReason()).isEqualTo("AVAILABLE");
		assertThat(response.getMaxPossibleServings()).isEqualTo(4L);
	}

	@Test
	void insufficientStockYieldsInsufficientStock() {
		Ingredient tomato = ingredient(1L, "Tomato", "100", true);
		when(menuItemRepository.findById(10L)).thenReturn(Optional.of(item));
		when(recipeIngredientRepository.findByMenuItemIdOrderByIngredientNameAsc(10L))
				.thenReturn(List.of(component(tomato, "250")));

		MenuAvailabilityResponse response = menuAvailabilityService.recalculateAvailability(10L);

		assertThat(response.isAvailable()).isFalse();
		assertThat(response.getAvailabilityReason()).isEqualTo("INSUFFICIENT_STOCK");
		assertThat(response.getMaxPossibleServings()).isZero();
	}

	@Test
	void inactiveIngredientYieldsInactiveIngredient() {
		Ingredient tomato = ingredient(1L, "Tomato", "1000", false);
		when(menuItemRepository.findById(10L)).thenReturn(Optional.of(item));
		when(recipeIngredientRepository.findByMenuItemIdOrderByIngredientNameAsc(10L))
				.thenReturn(List.of(component(tomato, "100")));

		MenuAvailabilityResponse response = menuAvailabilityService.recalculateAvailability(10L);

		assertThat(response.isAvailable()).isFalse();
		assertThat(response.getAvailabilityReason()).isEqualTo("INACTIVE_INGREDIENT");
		assertThat(response.getMaxPossibleServings()).isZero();
	}

	@Test
	void reasonPriorityPrefersManualOverNoRecipe() {
		item.setManualAvailable(false);
		when(menuItemRepository.findById(10L)).thenReturn(Optional.of(item));
		when(recipeIngredientRepository.findByMenuItemIdOrderByIngredientNameAsc(10L)).thenReturn(List.of());

		MenuAvailabilityResponse response = menuAvailabilityService.recalculateAvailability(10L);

		assertThat(response.getAvailabilityReason()).isEqualTo("MANUALLY_DISABLED");
	}

	@Test
	void reasonPriorityPrefersInactiveOverInsufficientStock() {
		Ingredient inactive = ingredient(1L, "Tomato", "10", false);
		Ingredient low = ingredient(2L, "Oil", "5", true);
		when(menuItemRepository.findById(10L)).thenReturn(Optional.of(item));
		when(recipeIngredientRepository.findByMenuItemIdOrderByIngredientNameAsc(10L))
				.thenReturn(List.of(component(inactive, "100"), component(low, "100")));

		MenuAvailabilityResponse response = menuAvailabilityService.recalculateAvailability(10L);

		assertThat(response.getAvailabilityReason()).isEqualTo("INACTIVE_INGREDIENT");
	}

	@Test
	void maxPossibleServingsUsesMinimumFloorRatio() {
		Ingredient a = ingredient(1L, "A", "1000", true);
		Ingredient b = ingredient(2L, "B", "500", true);
		when(menuItemRepository.findById(10L)).thenReturn(Optional.of(item));
		when(recipeIngredientRepository.findByMenuItemIdOrderByIngredientNameAsc(10L))
				.thenReturn(List.of(component(a, "250"), component(b, "100")));

		MenuAvailabilityResponse response = menuAvailabilityService.recalculateAvailability(10L);

		assertThat(response.getMaxPossibleServings()).isEqualTo(4L);
	}

	@Test
	void maxPossibleServingsUsesBigDecimalFloorNotFloatingPoint() {
		Ingredient ingredient = ingredient(1L, "Flour", "10.000", true);
		when(menuItemRepository.findById(10L)).thenReturn(Optional.of(item));
		when(recipeIngredientRepository.findByMenuItemIdOrderByIngredientNameAsc(10L))
				.thenReturn(List.of(component(ingredient, "3.000")));

		MenuAvailabilityResponse response = menuAvailabilityService.recalculateAvailability(10L);

		assertThat(response.getMaxPossibleServings()).isEqualTo(3L);
	}

	@Test
	void recalculateDoesNotChangeItemWhenAlreadyCorrect() {
		item.setAvailable(false);
		item.setAvailabilityReason(MenuAvailabilityReason.NO_RECIPE);
		when(menuItemRepository.findById(10L)).thenReturn(Optional.of(item));
		when(recipeIngredientRepository.findByMenuItemIdOrderByIngredientNameAsc(10L)).thenReturn(List.of());

		menuAvailabilityService.recalculateAvailability(10L);

		assertThat(item.isAvailable()).isFalse();
		assertThat(item.getAvailabilityReason()).isEqualTo(MenuAvailabilityReason.NO_RECIPE);
		verify(menuItemRepository, never()).save(any());
	}

	@Test
	void missingMenuItemThrows404() {
		when(menuItemRepository.findById(99L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> menuAvailabilityService.recalculateAvailability(99L))
				.isInstanceOf(ResourceNotFoundException.class)
				.hasMessageContaining("99");
	}

	@Test
	void recalculateForIngredientUpdatesOnlyAffectedItems() {
		MenuItem other = new MenuItem("Soup", null, new BigDecimal("5.00"), true, true, category);
		ReflectionTestUtils.setField(other, "id", 20L);

		when(recipeIngredientRepository.findDistinctMenuItemIdsByIngredientIdOrderByMenuItemIdAsc(1L))
				.thenReturn(List.of(10L));
		when(menuItemRepository.findById(10L)).thenReturn(Optional.of(item));
		when(recipeIngredientRepository.findByMenuItemIdOrderByIngredientNameAsc(10L)).thenReturn(List.of());

		List<MenuAvailabilityResponse> responses = menuAvailabilityService.recalculateForIngredient(1L);

		assertThat(responses).hasSize(1);
		assertThat(responses.get(0).getMenuItemId()).isEqualTo(10L);
		verify(menuItemRepository, never()).findById(20L);
	}

	@Test
	void evaluateAvailabilityDoesNotPersist() {
		when(menuItemRepository.findById(10L)).thenReturn(Optional.of(item));
		when(recipeIngredientRepository.findByMenuItemIdOrderByIngredientNameAsc(10L)).thenReturn(List.of());
		item.setAvailable(true);
		item.setAvailabilityReason(MenuAvailabilityReason.AVAILABLE);

		var evaluation = menuAvailabilityService.evaluateAvailability(10L);

		assertThat(evaluation.available()).isFalse();
		assertThat(evaluation.reason()).isEqualTo(MenuAvailabilityReason.NO_RECIPE);
		assertThat(item.isAvailable()).isTrue();
		assertThat(item.getAvailabilityReason()).isEqualTo(MenuAvailabilityReason.AVAILABLE);
	}

	private Ingredient ingredient(Long id, String name, String stock, boolean active) {
		Ingredient ingredient = new Ingredient(
				name,
				IngredientUnit.GRAM,
				new BigDecimal(stock),
				new BigDecimal("10"),
				active);
		ReflectionTestUtils.setField(ingredient, "id", id);
		return ingredient;
	}

	private RecipeIngredient component(Ingredient ingredient, String quantity) {
		return new RecipeIngredient(item, ingredient, new BigDecimal(quantity));
	}
}
