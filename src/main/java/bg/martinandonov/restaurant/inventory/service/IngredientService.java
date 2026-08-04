package bg.martinandonov.restaurant.inventory.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import bg.martinandonov.restaurant.common.exception.BusinessRuleException;
import bg.martinandonov.restaurant.common.exception.InvalidRequestException;
import bg.martinandonov.restaurant.common.exception.ResourceNotFoundException;
import bg.martinandonov.restaurant.inventory.dto.AdjustIngredientStockRequest;
import bg.martinandonov.restaurant.inventory.dto.CreateIngredientRequest;
import bg.martinandonov.restaurant.inventory.dto.IngredientResponse;
import bg.martinandonov.restaurant.inventory.dto.UpdateIngredientRequest;
import bg.martinandonov.restaurant.inventory.dto.UpdateIngredientStatusRequest;
import bg.martinandonov.restaurant.inventory.entity.Ingredient;
import bg.martinandonov.restaurant.inventory.entity.IngredientUnit;
import bg.martinandonov.restaurant.inventory.repository.IngredientRepository;
import bg.martinandonov.restaurant.inventory.repository.RecipeIngredientRepository;
import bg.martinandonov.restaurant.menu.service.MenuAvailabilityService;

@Service
@Transactional
public class IngredientService {

	private static final int MAX_NAME_LENGTH = 150;

	private final IngredientRepository ingredientRepository;
	private final RecipeIngredientRepository recipeIngredientRepository;
	private final MenuAvailabilityService menuAvailabilityService;

	public IngredientService(
			IngredientRepository ingredientRepository,
			RecipeIngredientRepository recipeIngredientRepository,
			MenuAvailabilityService menuAvailabilityService) {
		this.ingredientRepository = ingredientRepository;
		this.recipeIngredientRepository = recipeIngredientRepository;
		this.menuAvailabilityService = menuAvailabilityService;
	}

	public IngredientResponse createIngredient(CreateIngredientRequest request) {
		Objects.requireNonNull(request, "request must not be null");
		String name = requireName(request.getName());
		IngredientUnit unit = requireUnit(request.getUnit());
		BigDecimal stockQuantity = requireNonNegative(request.getStockQuantity(), "stockQuantity");
		BigDecimal minimumStockLevel = requireNonNegative(request.getMinimumStockLevel(), "minimumStockLevel");

		if (ingredientRepository.existsByNameIgnoreCase(name)) {
			throw new BusinessRuleException("An ingredient with this name already exists");
		}

		Ingredient ingredient = new Ingredient(name, unit, stockQuantity, minimumStockLevel, true);
		return toResponse(ingredientRepository.save(ingredient));
	}

	@Transactional(readOnly = true)
	public List<IngredientResponse> getAllIngredients() {
		return ingredientRepository.findAllByOrderByNameAsc().stream()
				.map(this::toResponse)
				.toList();
	}

	@Transactional(readOnly = true)
	public List<IngredientResponse> getActiveIngredients() {
		return ingredientRepository.findByActiveTrueOrderByNameAsc().stream()
				.map(this::toResponse)
				.toList();
	}

	@Transactional(readOnly = true)
	public IngredientResponse getIngredientById(Long id) {
		return toResponse(findIngredient(id));
	}

	public IngredientResponse updateIngredient(Long id, UpdateIngredientRequest request) {
		Objects.requireNonNull(request, "request must not be null");
		Ingredient ingredient = findIngredient(id);
		String name = requireName(request.getName());
		IngredientUnit unit = requireUnit(request.getUnit());
		BigDecimal minimumStockLevel = requireNonNegative(request.getMinimumStockLevel(), "minimumStockLevel");

		if (ingredientRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
			throw new BusinessRuleException("An ingredient with this name already exists");
		}

		if (unit != ingredient.getUnit() && recipeIngredientRepository.existsByIngredientId(id)) {
			throw new BusinessRuleException("Cannot change unit of an ingredient that is used in a recipe");
		}

		ingredient.setName(name);
		ingredient.setUnit(unit);
		ingredient.setMinimumStockLevel(minimumStockLevel);
		return toResponse(ingredient);
	}

	public IngredientResponse updateIngredientStatus(Long id, UpdateIngredientStatusRequest request) {
		Objects.requireNonNull(request, "request must not be null");
		if (request.getActive() == null) {
			throw new InvalidRequestException("active must be provided");
		}
		Ingredient ingredient = findIngredient(id);
		ingredient.setActive(request.getActive());
		menuAvailabilityService.recalculateForIngredient(id);
		return toResponse(ingredient);
	}

	public IngredientResponse adjustStock(Long id, AdjustIngredientStockRequest request) {
		Objects.requireNonNull(request, "request must not be null");
		if (request.getQuantityChange() == null) {
			throw new InvalidRequestException("quantityChange must be provided");
		}

		Ingredient ingredient = ingredientRepository.findByIdForUpdate(id)
				.orElseThrow(() -> new ResourceNotFoundException("Ingredient not found: " + id));

		BigDecimal updated = ingredient.getStockQuantity().add(request.getQuantityChange());
		if (updated.compareTo(BigDecimal.ZERO) < 0) {
			throw new InvalidRequestException("Stock quantity cannot become negative");
		}

		ingredient.setStockQuantity(updated);
		menuAvailabilityService.recalculateForIngredient(id);
		return toResponse(ingredient);
	}

	@Transactional(readOnly = true)
	public Ingredient getActiveIngredientEntity(Long id) {
		Ingredient ingredient = findIngredient(id);
		if (!ingredient.isActive()) {
			throw new BusinessRuleException("Ingredient is inactive: " + id);
		}
		return ingredient;
	}

	@Transactional(readOnly = true)
	public Ingredient getIngredientEntity(Long id) {
		return findIngredient(id);
	}

	private Ingredient findIngredient(Long id) {
		if (id == null) {
			throw new InvalidRequestException("Ingredient id must be provided");
		}
		return ingredientRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Ingredient not found: " + id));
	}

	private String requireName(String name) {
		if (name == null || name.isBlank()) {
			throw new InvalidRequestException("Ingredient name must not be blank");
		}
		String trimmed = name.trim();
		if (trimmed.length() > MAX_NAME_LENGTH) {
			throw new InvalidRequestException("Ingredient name must be at most " + MAX_NAME_LENGTH + " characters");
		}
		return trimmed;
	}

	private IngredientUnit requireUnit(String unit) {
		if (unit == null || unit.isBlank()) {
			throw new InvalidRequestException("Ingredient unit must be provided");
		}
		try {
			return IngredientUnit.valueOf(unit.trim().toUpperCase(Locale.ROOT));
		}
		catch (IllegalArgumentException ex) {
			throw new InvalidRequestException("Unknown ingredient unit: " + unit.trim());
		}
	}

	private BigDecimal requireNonNegative(BigDecimal value, String fieldName) {
		if (value == null) {
			throw new InvalidRequestException(fieldName + " must be provided");
		}
		if (value.compareTo(BigDecimal.ZERO) < 0) {
			throw new InvalidRequestException(fieldName + " must not be negative");
		}
		return value;
	}

	private IngredientResponse toResponse(Ingredient ingredient) {
		boolean lowStock = ingredient.getStockQuantity().compareTo(ingredient.getMinimumStockLevel()) <= 0;
		return new IngredientResponse(
				ingredient.getId(),
				ingredient.getName(),
				ingredient.getUnit().name(),
				ingredient.getStockQuantity(),
				ingredient.getMinimumStockLevel(),
				ingredient.isActive(),
				lowStock);
	}
}
