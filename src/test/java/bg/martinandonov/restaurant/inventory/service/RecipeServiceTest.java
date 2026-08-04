package bg.martinandonov.restaurant.inventory.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import bg.martinandonov.restaurant.common.exception.BusinessRuleException;
import bg.martinandonov.restaurant.common.exception.InvalidRequestException;
import bg.martinandonov.restaurant.common.exception.ResourceNotFoundException;
import bg.martinandonov.restaurant.inventory.dto.RecipeComponentRequest;
import bg.martinandonov.restaurant.inventory.dto.RecipeResponse;
import bg.martinandonov.restaurant.inventory.dto.ReplaceRecipeRequest;
import bg.martinandonov.restaurant.inventory.entity.Ingredient;
import bg.martinandonov.restaurant.inventory.entity.IngredientUnit;
import bg.martinandonov.restaurant.inventory.entity.RecipeIngredient;
import bg.martinandonov.restaurant.inventory.repository.RecipeIngredientRepository;
import bg.martinandonov.restaurant.menu.entity.MenuCategory;
import bg.martinandonov.restaurant.menu.entity.MenuItem;
import bg.martinandonov.restaurant.menu.repository.MenuItemRepository;

@ExtendWith(MockitoExtension.class)
class RecipeServiceTest {

	@Mock
	private RecipeIngredientRepository recipeIngredientRepository;

	@Mock
	private MenuItemRepository menuItemRepository;

	@Mock
	private IngredientService ingredientService;

	@InjectMocks
	private RecipeService recipeService;

	private MenuItem menuItem;
	private Ingredient tomato;
	private Ingredient oil;

	@BeforeEach
	void setUp() {
		MenuCategory category = new MenuCategory("Salads", null, true);
		ReflectionTestUtils.setField(category, "id", 1L);
		menuItem = new MenuItem("Caesar Salad", null, new BigDecimal("12.50"), true, true, category);
		ReflectionTestUtils.setField(menuItem, "id", 10L);

		tomato = new Ingredient("Tomato", IngredientUnit.GRAM, new BigDecimal("5000"), new BigDecimal("500"), true);
		ReflectionTestUtils.setField(tomato, "id", 1L);
		oil = new Ingredient("Oil", IngredientUnit.MILLILITER, new BigDecimal("2000"), new BigDecimal("200"), true);
		ReflectionTestUtils.setField(oil, "id", 2L);
	}

	@Test
	void replaceRecipeCreatesComponentsForExistingMenuItem() {
		when(menuItemRepository.findById(10L)).thenReturn(Optional.of(menuItem));
		when(ingredientService.getActiveIngredientEntity(1L)).thenReturn(tomato);
		when(ingredientService.getActiveIngredientEntity(2L)).thenReturn(oil);
		when(recipeIngredientRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

		RecipeResponse response = recipeService.replaceRecipeForMenuItem(10L, replaceRequest(
				component(1L, "150"),
				component(2L, "20")));

		verify(recipeIngredientRepository).deleteByMenuItemId(10L);
		verify(recipeIngredientRepository).saveAll(anyList());
		assertThat(response.getMenuItemId()).isEqualTo(10L);
		assertThat(response.getComponents()).hasSize(2);
	}

	@Test
	void replaceRecipeRejectsMissingMenuItem() {
		when(menuItemRepository.findById(99L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> recipeService.replaceRecipeForMenuItem(99L, replaceRequest(component(1L, "10"))))
				.isInstanceOf(ResourceNotFoundException.class)
				.hasMessageContaining("99");
		verify(recipeIngredientRepository, never()).deleteByMenuItemId(99L);
	}

	@Test
	void replaceRecipeRejectsMissingIngredient() {
		when(menuItemRepository.findById(10L)).thenReturn(Optional.of(menuItem));
		when(ingredientService.getActiveIngredientEntity(1L))
				.thenThrow(new ResourceNotFoundException("Ingredient not found: 1"));

		assertThatThrownBy(() -> recipeService.replaceRecipeForMenuItem(10L, replaceRequest(component(1L, "10"))))
				.isInstanceOf(ResourceNotFoundException.class);
		verify(recipeIngredientRepository, never()).deleteByMenuItemId(10L);
	}

	@Test
	void replaceRecipeRejectsInactiveIngredient() {
		when(menuItemRepository.findById(10L)).thenReturn(Optional.of(menuItem));
		when(ingredientService.getActiveIngredientEntity(1L))
				.thenThrow(new BusinessRuleException("Ingredient is inactive: 1"));

		assertThatThrownBy(() -> recipeService.replaceRecipeForMenuItem(10L, replaceRequest(component(1L, "10"))))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("inactive");
		verify(recipeIngredientRepository, never()).deleteByMenuItemId(10L);
	}

	@Test
	void replaceRecipeRejectsDuplicateIngredientIds() {
		when(menuItemRepository.findById(10L)).thenReturn(Optional.of(menuItem));

		assertThatThrownBy(() -> recipeService.replaceRecipeForMenuItem(
				10L,
				replaceRequest(component(1L, "10"), component(1L, "20"))))
				.isInstanceOf(InvalidRequestException.class)
				.hasMessageContaining("Duplicate");
		verify(recipeIngredientRepository, never()).deleteByMenuItemId(10L);
	}

	@Test
	void replaceRecipeRejectsNonPositiveQuantity() {
		when(menuItemRepository.findById(10L)).thenReturn(Optional.of(menuItem));

		assertThatThrownBy(() -> recipeService.replaceRecipeForMenuItem(10L, replaceRequest(component(1L, "0"))))
				.isInstanceOf(InvalidRequestException.class)
				.hasMessageContaining("greater than 0");
		verify(recipeIngredientRepository, never()).deleteByMenuItemId(10L);
	}

	@Test
	void replaceRecipeRejectsEmptyRecipe() {
		when(menuItemRepository.findById(10L)).thenReturn(Optional.of(menuItem));

		ReplaceRecipeRequest request = new ReplaceRecipeRequest();
		request.setComponents(List.of());

		assertThatThrownBy(() -> recipeService.replaceRecipeForMenuItem(10L, request))
				.isInstanceOf(InvalidRequestException.class)
				.hasMessageContaining("at least one");
		verify(recipeIngredientRepository, never()).deleteByMenuItemId(10L);
	}

	@Test
	void failedValidationDoesNotDeleteExistingRecipe() {
		when(menuItemRepository.findById(10L)).thenReturn(Optional.of(menuItem));

		assertThatThrownBy(() -> recipeService.replaceRecipeForMenuItem(
				10L,
				replaceRequest(component(1L, "-1"))))
				.isInstanceOf(InvalidRequestException.class);

		verify(recipeIngredientRepository, never()).deleteByMenuItemId(10L);
		verify(recipeIngredientRepository, never()).saveAll(anyList());
	}

	@Test
	void removeRecipeDeletesOnlyRecipeComponents() {
		when(menuItemRepository.findById(10L)).thenReturn(Optional.of(menuItem));

		recipeService.removeRecipeForMenuItem(10L);

		verify(recipeIngredientRepository).deleteByMenuItemId(10L);
		verify(menuItemRepository, never()).delete(menuItem);
	}

	@Test
	void getRecipeReturnsOrderedComponents() {
		when(menuItemRepository.findById(10L)).thenReturn(Optional.of(menuItem));
		RecipeIngredient component = new RecipeIngredient(menuItem, tomato, new BigDecimal("100"));
		when(recipeIngredientRepository.findByMenuItemIdOrderByIngredientNameAsc(10L))
				.thenReturn(List.of(component));

		RecipeResponse response = recipeService.getRecipeForMenuItem(10L);

		assertThat(response.getMenuItemName()).isEqualTo("Caesar Salad");
		assertThat(response.getComponents()).hasSize(1);
		assertThat(response.getComponents().get(0).getIngredientName()).isEqualTo("Tomato");
	}

	private ReplaceRecipeRequest replaceRequest(RecipeComponentRequest... components) {
		ReplaceRecipeRequest request = new ReplaceRecipeRequest();
		request.setComponents(List.of(components));
		return request;
	}

	private RecipeComponentRequest component(Long ingredientId, String quantity) {
		RecipeComponentRequest request = new RecipeComponentRequest();
		request.setIngredientId(ingredientId);
		request.setQuantityRequired(new BigDecimal(quantity));
		return request;
	}
}
