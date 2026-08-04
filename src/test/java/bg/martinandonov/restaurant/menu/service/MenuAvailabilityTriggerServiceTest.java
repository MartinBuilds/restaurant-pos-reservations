package bg.martinandonov.restaurant.menu.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import bg.martinandonov.restaurant.common.exception.InvalidRequestException;
import bg.martinandonov.restaurant.inventory.dto.AdjustIngredientStockRequest;
import bg.martinandonov.restaurant.inventory.dto.RecipeComponentRequest;
import bg.martinandonov.restaurant.inventory.dto.ReplaceRecipeRequest;
import bg.martinandonov.restaurant.inventory.dto.UpdateIngredientStatusRequest;
import bg.martinandonov.restaurant.inventory.entity.Ingredient;
import bg.martinandonov.restaurant.inventory.entity.IngredientUnit;
import bg.martinandonov.restaurant.inventory.entity.RecipeIngredient;
import bg.martinandonov.restaurant.inventory.repository.IngredientRepository;
import bg.martinandonov.restaurant.inventory.repository.RecipeIngredientRepository;
import bg.martinandonov.restaurant.inventory.service.IngredientService;
import bg.martinandonov.restaurant.inventory.service.RecipeService;
import bg.martinandonov.restaurant.menu.dto.CreateMenuItemRequest;
import bg.martinandonov.restaurant.menu.dto.MenuItemResponse;
import bg.martinandonov.restaurant.menu.dto.UpdateMenuItemAvailabilityRequest;
import bg.martinandonov.restaurant.menu.entity.MenuAvailabilityReason;
import bg.martinandonov.restaurant.menu.entity.MenuCategory;
import bg.martinandonov.restaurant.menu.entity.MenuItem;
import bg.martinandonov.restaurant.menu.repository.MenuItemRepository;

@ExtendWith(MockitoExtension.class)
class MenuAvailabilityTriggerServiceTest {

	@Mock
	private MenuItemRepository menuItemRepository;

	@Mock
	private RecipeIngredientRepository recipeIngredientRepository;

	@Mock
	private IngredientRepository ingredientRepository;

	@Mock
	private MenuCategoryService menuCategoryService;

	private MenuAvailabilityService menuAvailabilityService;
	private IngredientService ingredientService;
	private RecipeService recipeService;
	private MenuItemService menuItemService;

	private MenuCategory category;
	private MenuItem menuItem;
	private Ingredient tomato;

	@BeforeEach
	void setUp() {
		menuAvailabilityService = new MenuAvailabilityService(menuItemRepository, recipeIngredientRepository);
		ingredientService = new IngredientService(
				ingredientRepository,
				recipeIngredientRepository,
				menuAvailabilityService);
		recipeService = new RecipeService(
				recipeIngredientRepository,
				menuItemRepository,
				ingredientService,
				menuAvailabilityService);
		menuItemService = new MenuItemService(
				menuItemRepository,
				menuCategoryService,
				menuAvailabilityService);

		category = new MenuCategory("Salads", null, true);
		ReflectionTestUtils.setField(category, "id", 1L);
		menuItem = new MenuItem("Caesar Salad", null, new BigDecimal("12.50"), true, true, category);
		ReflectionTestUtils.setField(menuItem, "id", 10L);
		tomato = new Ingredient("Tomato", IngredientUnit.GRAM, new BigDecimal("1000"), new BigDecimal("100"), true);
		ReflectionTestUtils.setField(tomato, "id", 1L);
		ReflectionTestUtils.setField(tomato, "version", 0L);
	}

	@Test
	void stockAdjustmentFromSufficientToInsufficientMakesItemUnavailable() {
		stubRecipeWithStock("1000");
		menuAvailabilityService.recalculateAvailability(10L);
		assertThat(menuItem.isAvailable()).isTrue();

		tomato.setStockQuantity(new BigDecimal("1000"));
		when(ingredientRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(tomato));
		when(recipeIngredientRepository.findDistinctMenuItemIdsByIngredientIdOrderByMenuItemIdAsc(1L))
				.thenReturn(List.of(10L));
		when(recipeIngredientRepository.findByMenuItemIdOrderByIngredientNameAsc(10L))
				.thenAnswer(invocation -> List.of(new RecipeIngredient(menuItem, tomato, new BigDecimal("250"))));

		AdjustIngredientStockRequest request = new AdjustIngredientStockRequest();
		request.setQuantityChange(new BigDecimal("-900"));
		ingredientService.adjustStock(1L, request);

		assertThat(menuItem.isAvailable()).isFalse();
		assertThat(menuItem.getAvailabilityReason()).isEqualTo(MenuAvailabilityReason.INSUFFICIENT_STOCK);
	}

	@Test
	void stockAdjustmentFromInsufficientToSufficientMakesItemAvailable() {
		tomato.setStockQuantity(new BigDecimal("100"));
		stubRecipeWithStock("100");
		menuAvailabilityService.recalculateAvailability(10L);
		assertThat(menuItem.isAvailable()).isFalse();

		when(ingredientRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(tomato));
		when(recipeIngredientRepository.findDistinctMenuItemIdsByIngredientIdOrderByMenuItemIdAsc(1L))
				.thenReturn(List.of(10L));
		when(recipeIngredientRepository.findByMenuItemIdOrderByIngredientNameAsc(10L))
				.thenAnswer(invocation -> List.of(new RecipeIngredient(menuItem, tomato, new BigDecimal("250"))));

		AdjustIngredientStockRequest request = new AdjustIngredientStockRequest();
		request.setQuantityChange(new BigDecimal("400"));
		ingredientService.adjustStock(1L, request);

		assertThat(menuItem.isAvailable()).isTrue();
		assertThat(menuItem.getAvailabilityReason()).isEqualTo(MenuAvailabilityReason.AVAILABLE);
	}

	@Test
	void ingredientDeactivationMakesItemUnavailable() {
		stubRecipeWithStock("1000");
		menuAvailabilityService.recalculateAvailability(10L);
		assertThat(menuItem.isAvailable()).isTrue();

		when(ingredientRepository.findById(1L)).thenReturn(Optional.of(tomato));
		when(recipeIngredientRepository.findDistinctMenuItemIdsByIngredientIdOrderByMenuItemIdAsc(1L))
				.thenReturn(List.of(10L));
		when(recipeIngredientRepository.findByMenuItemIdOrderByIngredientNameAsc(10L))
				.thenAnswer(invocation -> List.of(new RecipeIngredient(menuItem, tomato, new BigDecimal("250"))));

		UpdateIngredientStatusRequest request = new UpdateIngredientStatusRequest();
		request.setActive(false);
		ingredientService.updateIngredientStatus(1L, request);

		assertThat(menuItem.isAvailable()).isFalse();
		assertThat(menuItem.getAvailabilityReason()).isEqualTo(MenuAvailabilityReason.INACTIVE_INGREDIENT);
	}

	@Test
	void ingredientReactivationRestoresAvailabilityOnlyWhenStockSufficient() {
		tomato.setActive(false);
		tomato.setStockQuantity(new BigDecimal("1000"));
		stubRecipeWithStock("1000");
		menuItem.setAvailabilityReason(MenuAvailabilityReason.INACTIVE_INGREDIENT);

		when(ingredientRepository.findById(1L)).thenReturn(Optional.of(tomato));
		when(recipeIngredientRepository.findDistinctMenuItemIdsByIngredientIdOrderByMenuItemIdAsc(1L))
				.thenReturn(List.of(10L));
		when(recipeIngredientRepository.findByMenuItemIdOrderByIngredientNameAsc(10L))
				.thenAnswer(invocation -> List.of(new RecipeIngredient(menuItem, tomato, new BigDecimal("250"))));

		UpdateIngredientStatusRequest request = new UpdateIngredientStatusRequest();
		request.setActive(true);
		ingredientService.updateIngredientStatus(1L, request);

		assertThat(menuItem.isAvailable()).isTrue();
		assertThat(menuItem.getAvailabilityReason()).isEqualTo(MenuAvailabilityReason.AVAILABLE);
	}

	@Test
	void replaceRecipeRecalculatesAvailability() {
		when(menuItemRepository.findById(10L)).thenReturn(Optional.of(menuItem));
		when(ingredientRepository.findById(1L)).thenReturn(Optional.of(tomato));
		when(recipeIngredientRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));
		when(recipeIngredientRepository.findByMenuItemIdOrderByIngredientNameAsc(10L))
				.thenAnswer(invocation -> List.of(new RecipeIngredient(menuItem, tomato, new BigDecimal("250"))));

		recipeService.replaceRecipeForMenuItem(10L, replaceRequest(component(1L, "250")));

		assertThat(menuItem.isAvailable()).isTrue();
		assertThat(menuItem.getAvailabilityReason()).isEqualTo(MenuAvailabilityReason.AVAILABLE);
	}

	@Test
	void removeRecipeSetsNoRecipe() {
		menuItem.setAvailable(true);
		menuItem.setAvailabilityReason(MenuAvailabilityReason.AVAILABLE);
		when(menuItemRepository.findById(10L)).thenReturn(Optional.of(menuItem));
		when(recipeIngredientRepository.findByMenuItemIdOrderByIngredientNameAsc(10L)).thenReturn(List.of());

		recipeService.removeRecipeForMenuItem(10L);

		assertThat(menuItem.isAvailable()).isFalse();
		assertThat(menuItem.getAvailabilityReason()).isEqualTo(MenuAvailabilityReason.NO_RECIPE);
	}

	@Test
	void failedRecipeReplacementDoesNotChangeAvailability() {
		menuItem.setAvailable(true);
		menuItem.setAvailabilityReason(MenuAvailabilityReason.AVAILABLE);
		when(menuItemRepository.findById(10L)).thenReturn(Optional.of(menuItem));

		try {
			ReplaceRecipeRequest request = new ReplaceRecipeRequest();
			request.setComponents(List.of());
			recipeService.replaceRecipeForMenuItem(10L, request);
		}
		catch (InvalidRequestException ignored) {
			// expected
		}

		verify(recipeIngredientRepository, never()).deleteByMenuItemId(10L);
		assertThat(menuItem.isAvailable()).isTrue();
		assertThat(menuItem.getAvailabilityReason()).isEqualTo(MenuAvailabilityReason.AVAILABLE);
	}

	@Test
	void newMenuItemWithoutRecipeStartsUnavailable() {
		when(menuCategoryService.getCategoryEntity(1L)).thenReturn(category);
		when(menuItemRepository.existsByCategoryIdAndNameIgnoreCase(1L, "Soup")).thenReturn(false);
		when(menuItemRepository.save(any(MenuItem.class))).thenAnswer(invocation -> {
			MenuItem saved = invocation.getArgument(0);
			ReflectionTestUtils.setField(saved, "id", 11L);
			return saved;
		});
		when(menuItemRepository.findById(11L)).thenAnswer(invocation -> {
			MenuItem loaded = new MenuItem("Soup", null, new BigDecimal("5.00"), true, true, category);
			ReflectionTestUtils.setField(loaded, "id", 11L);
			return Optional.of(loaded);
		});
		when(recipeIngredientRepository.findByMenuItemIdOrderByIngredientNameAsc(11L)).thenReturn(List.of());

		CreateMenuItemRequest request = new CreateMenuItemRequest();
		request.setName("Soup");
		request.setPrice(new BigDecimal("5.00"));
		request.setCategoryId(1L);

		MenuItemResponse response = menuItemService.createMenuItem(request);

		assertThat(response.isManualAvailable()).isTrue();
		assertThat(response.isAvailable()).isFalse();
		assertThat(response.getAvailabilityReason()).isEqualTo("NO_RECIPE");
	}

	@Test
	void manualAvailabilityFalseKeepsItemUnavailableEvenWithStock() {
		stubRecipeWithStock("1000");
		menuAvailabilityService.recalculateAvailability(10L);
		assertThat(menuItem.isAvailable()).isTrue();

		when(menuItemRepository.findById(10L)).thenReturn(Optional.of(menuItem));
		when(recipeIngredientRepository.findByMenuItemIdOrderByIngredientNameAsc(10L))
				.thenReturn(List.of(new RecipeIngredient(menuItem, tomato, new BigDecimal("250"))));

		UpdateMenuItemAvailabilityRequest request = new UpdateMenuItemAvailabilityRequest();
		request.setAvailable(false);
		MenuItemResponse response = menuItemService.updateAvailability(10L, request);

		assertThat(response.isManualAvailable()).isFalse();
		assertThat(response.isAvailable()).isFalse();
		assertThat(response.getAvailabilityReason()).isEqualTo("MANUALLY_DISABLED");
	}

	@Test
	void manualAvailabilityTrueDoesNotMakeItemAvailableWhenStockInsufficient() {
		tomato.setStockQuantity(new BigDecimal("50"));
		menuItem.setManualAvailable(false);
		menuItem.setAvailable(false);
		menuItem.setAvailabilityReason(MenuAvailabilityReason.MANUALLY_DISABLED);
		when(menuItemRepository.findById(10L)).thenReturn(Optional.of(menuItem));
		when(recipeIngredientRepository.findByMenuItemIdOrderByIngredientNameAsc(10L))
				.thenReturn(List.of(new RecipeIngredient(menuItem, tomato, new BigDecimal("250"))));

		UpdateMenuItemAvailabilityRequest request = new UpdateMenuItemAvailabilityRequest();
		request.setAvailable(true);
		MenuItemResponse response = menuItemService.updateAvailability(10L, request);

		assertThat(response.isManualAvailable()).isTrue();
		assertThat(response.isAvailable()).isFalse();
		assertThat(response.getAvailabilityReason()).isEqualTo("INSUFFICIENT_STOCK");
	}

	@Test
	void manualAvailabilityTrueRestoresItemWhenRecipeAndStockValid() {
		menuItem.setManualAvailable(false);
		menuItem.setAvailable(false);
		menuItem.setAvailabilityReason(MenuAvailabilityReason.MANUALLY_DISABLED);
		when(menuItemRepository.findById(10L)).thenReturn(Optional.of(menuItem));
		when(recipeIngredientRepository.findByMenuItemIdOrderByIngredientNameAsc(10L))
				.thenReturn(List.of(new RecipeIngredient(menuItem, tomato, new BigDecimal("250"))));

		UpdateMenuItemAvailabilityRequest request = new UpdateMenuItemAvailabilityRequest();
		request.setAvailable(true);
		MenuItemResponse response = menuItemService.updateAvailability(10L, request);

		assertThat(response.isManualAvailable()).isTrue();
		assertThat(response.isAvailable()).isTrue();
		assertThat(response.getAvailabilityReason()).isEqualTo("AVAILABLE");
	}

	private void stubRecipeWithStock(String stock) {
		tomato.setStockQuantity(new BigDecimal(stock));
		when(menuItemRepository.findById(10L)).thenReturn(Optional.of(menuItem));
		when(recipeIngredientRepository.findByMenuItemIdOrderByIngredientNameAsc(10L))
				.thenReturn(List.of(new RecipeIngredient(menuItem, tomato, new BigDecimal("250"))));
	}

	private ReplaceRecipeRequest replaceRequest(RecipeComponentRequest component) {
		ReplaceRecipeRequest request = new ReplaceRecipeRequest();
		request.setComponents(List.of(component));
		return request;
	}

	private RecipeComponentRequest component(Long ingredientId, String quantity) {
		RecipeComponentRequest request = new RecipeComponentRequest();
		request.setIngredientId(ingredientId);
		request.setQuantityRequired(new BigDecimal(quantity));
		return request;
	}
}
