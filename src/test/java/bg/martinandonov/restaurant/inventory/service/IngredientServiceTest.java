package bg.martinandonov.restaurant.inventory.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import bg.martinandonov.restaurant.common.exception.BusinessRuleException;
import bg.martinandonov.restaurant.common.exception.InvalidRequestException;
import bg.martinandonov.restaurant.common.exception.ResourceNotFoundException;
import bg.martinandonov.restaurant.inventory.dto.AdjustIngredientStockRequest;
import bg.martinandonov.restaurant.inventory.dto.CreateIngredientRequest;
import bg.martinandonov.restaurant.inventory.dto.IngredientResponse;
import bg.martinandonov.restaurant.inventory.dto.UpdateIngredientRequest;
import bg.martinandonov.restaurant.inventory.entity.Ingredient;
import bg.martinandonov.restaurant.inventory.entity.IngredientUnit;
import bg.martinandonov.restaurant.inventory.repository.IngredientRepository;
import bg.martinandonov.restaurant.inventory.repository.RecipeIngredientRepository;

@ExtendWith(MockitoExtension.class)
class IngredientServiceTest {

	@Mock
	private IngredientRepository ingredientRepository;

	@Mock
	private RecipeIngredientRepository recipeIngredientRepository;

	@InjectMocks
	private IngredientService ingredientService;

	@Test
	void createIngredientTrimsName() {
		CreateIngredientRequest request = new CreateIngredientRequest();
		request.setName("  Tomato  ");
		request.setUnit("GRAM");
		request.setStockQuantity(new BigDecimal("1000"));
		request.setMinimumStockLevel(new BigDecimal("100"));

		when(ingredientRepository.existsByNameIgnoreCase("Tomato")).thenReturn(false);
		when(ingredientRepository.save(any(Ingredient.class))).thenAnswer(invocation -> {
			Ingredient ingredient = invocation.getArgument(0);
			ReflectionTestUtils.setField(ingredient, "id", 1L);
			return ingredient;
		});

		IngredientResponse response = ingredientService.createIngredient(request);

		ArgumentCaptor<Ingredient> captor = ArgumentCaptor.forClass(Ingredient.class);
		verify(ingredientRepository).save(captor.capture());
		assertThat(captor.getValue().getName()).isEqualTo("Tomato");
		assertThat(response.getName()).isEqualTo("Tomato");
		assertThat(response.isLowStock()).isFalse();
	}

	@Test
	void createIngredientRejectsDuplicateName() {
		CreateIngredientRequest request = baseCreateRequest();
		when(ingredientRepository.existsByNameIgnoreCase("Tomato")).thenReturn(true);

		assertThatThrownBy(() -> ingredientService.createIngredient(request))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("already exists");
		verify(ingredientRepository, never()).save(any());
	}

	@Test
	void createIngredientRejectsNegativeStock() {
		CreateIngredientRequest request = baseCreateRequest();
		request.setStockQuantity(new BigDecimal("-1"));

		assertThatThrownBy(() -> ingredientService.createIngredient(request))
				.isInstanceOf(InvalidRequestException.class)
				.hasMessageContaining("stockQuantity");
	}

	@Test
	void createIngredientRejectsNegativeMinimumStock() {
		CreateIngredientRequest request = baseCreateRequest();
		request.setMinimumStockLevel(new BigDecimal("-5"));

		assertThatThrownBy(() -> ingredientService.createIngredient(request))
				.isInstanceOf(InvalidRequestException.class)
				.hasMessageContaining("minimumStockLevel");
	}

	@Test
	void adjustStockIncreasesQuantity() {
		Ingredient ingredient = existingIngredient();
		when(ingredientRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(ingredient));

		AdjustIngredientStockRequest request = new AdjustIngredientStockRequest();
		request.setQuantityChange(new BigDecimal("250"));

		IngredientResponse response = ingredientService.adjustStock(1L, request);

		assertThat(response.getStockQuantity()).isEqualByComparingTo("1250");
		assertThat(response.isLowStock()).isFalse();
	}

	@Test
	void adjustStockDecreasesQuantity() {
		Ingredient ingredient = existingIngredient();
		when(ingredientRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(ingredient));

		AdjustIngredientStockRequest request = new AdjustIngredientStockRequest();
		request.setQuantityChange(new BigDecimal("-200"));

		IngredientResponse response = ingredientService.adjustStock(1L, request);

		assertThat(response.getStockQuantity()).isEqualByComparingTo("800");
	}

	@Test
	void adjustStockRejectsNegativeResult() {
		Ingredient ingredient = existingIngredient();
		when(ingredientRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(ingredient));

		AdjustIngredientStockRequest request = new AdjustIngredientStockRequest();
		request.setQuantityChange(new BigDecimal("-2000"));

		assertThatThrownBy(() -> ingredientService.adjustStock(1L, request))
				.isInstanceOf(InvalidRequestException.class)
				.hasMessageContaining("negative");
	}

	@Test
	void lowStockIsCalculatedCorrectly() {
		Ingredient ingredient = existingIngredient();
		ingredient.setStockQuantity(new BigDecimal("100"));
		ingredient.setMinimumStockLevel(new BigDecimal("100"));
		when(ingredientRepository.findById(1L)).thenReturn(Optional.of(ingredient));

		IngredientResponse response = ingredientService.getIngredientById(1L);

		assertThat(response.isLowStock()).isTrue();
	}

	@Test
	void getIngredientByIdThrowsWhenMissing() {
		when(ingredientRepository.findById(99L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> ingredientService.getIngredientById(99L))
				.isInstanceOf(ResourceNotFoundException.class)
				.hasMessageContaining("99");
	}

	@Test
	void updateRejectsUnitChangeWhenUsedInRecipe() {
		Ingredient ingredient = existingIngredient();
		when(ingredientRepository.findById(1L)).thenReturn(Optional.of(ingredient));
		when(ingredientRepository.existsByNameIgnoreCaseAndIdNot("Tomato", 1L)).thenReturn(false);
		when(recipeIngredientRepository.existsByIngredientId(1L)).thenReturn(true);

		UpdateIngredientRequest request = new UpdateIngredientRequest();
		request.setName("Tomato");
		request.setUnit("PIECE");
		request.setMinimumStockLevel(new BigDecimal("100"));

		assertThatThrownBy(() -> ingredientService.updateIngredient(1L, request))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("Cannot change unit");
	}

	private CreateIngredientRequest baseCreateRequest() {
		CreateIngredientRequest request = new CreateIngredientRequest();
		request.setName("Tomato");
		request.setUnit("GRAM");
		request.setStockQuantity(new BigDecimal("1000"));
		request.setMinimumStockLevel(new BigDecimal("100"));
		return request;
	}

	private Ingredient existingIngredient() {
		Ingredient ingredient = new Ingredient(
				"Tomato",
				IngredientUnit.GRAM,
				new BigDecimal("1000"),
				new BigDecimal("100"),
				true);
		ReflectionTestUtils.setField(ingredient, "id", 1L);
		ReflectionTestUtils.setField(ingredient, "version", 0L);
		return ingredient;
	}
}
