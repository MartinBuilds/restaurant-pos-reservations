package bg.martinandonov.restaurant.inventory.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import bg.martinandonov.restaurant.common.exception.BusinessRuleException;
import bg.martinandonov.restaurant.common.exception.GlobalExceptionHandler;
import bg.martinandonov.restaurant.common.exception.InvalidRequestException;
import bg.martinandonov.restaurant.common.exception.ResourceNotFoundException;
import bg.martinandonov.restaurant.inventory.dto.IngredientResponse;
import bg.martinandonov.restaurant.inventory.service.IngredientService;
import bg.martinandonov.restaurant.inventory.service.RecipeService;
import bg.martinandonov.restaurant.security.SecurityConfig;

@WebMvcTest(controllers = { AdminIngredientController.class, AdminRecipeController.class })
@Import({ SecurityConfig.class, GlobalExceptionHandler.class })
class InventoryControllerSecurityTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private IngredientService ingredientService;

	@MockitoBean
	private RecipeService recipeService;

	@MockitoBean
	private UserDetailsService userDetailsService;

	@Test
	@WithAnonymousUser
	void anonymousCannotAccessIngredients() throws Exception {
		mockMvc.perform(get("/api/admin/inventory/ingredients"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	@WithMockUser(roles = "WAITER")
	void waiterCannotAccessIngredients() throws Exception {
		mockMvc.perform(get("/api/admin/inventory/ingredients"))
				.andExpect(status().isForbidden());
	}

	@Test
	@WithMockUser(roles = "COOK")
	void cookCannotAccessIngredients() throws Exception {
		mockMvc.perform(get("/api/admin/inventory/ingredients"))
				.andExpect(status().isForbidden());
	}

	@Test
	@WithMockUser(roles = "ADMIN")
	void adminCanAccessIngredients() throws Exception {
		when(ingredientService.getAllIngredients()).thenReturn(List.of(
				new IngredientResponse(
						1L,
						"Tomato",
						"GRAM",
						new BigDecimal("1000"),
						new BigDecimal("100"),
						true,
						false)));

		mockMvc.perform(get("/api/admin/inventory/ingredients"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].name").value("Tomato"));
	}

	@Test
	@WithMockUser(roles = "ADMIN")
	void createIngredientReturns201() throws Exception {
		when(ingredientService.createIngredient(any())).thenReturn(
				new IngredientResponse(
						1L,
						"Tomato",
						"GRAM",
						new BigDecimal("1000"),
						new BigDecimal("100"),
						true,
						false));

		mockMvc.perform(post("/api/admin/inventory/ingredients")
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "name": "Tomato",
								  "unit": "GRAM",
								  "stockQuantity": 1000,
								  "minimumStockLevel": 100
								}
								"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.name").value("Tomato"));
	}

	@Test
	@WithMockUser(roles = "ADMIN")
	void invalidRequestReturns400() throws Exception {
		when(ingredientService.createIngredient(any()))
				.thenThrow(new InvalidRequestException("stockQuantity must not be negative"));

		mockMvc.perform(post("/api/admin/inventory/ingredients")
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "name": "Tomato",
								  "unit": "GRAM",
								  "stockQuantity": -1,
								  "minimumStockLevel": 100
								}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status").value(400));
	}

	@Test
	@WithMockUser(roles = "ADMIN")
	void missingResourceReturns404() throws Exception {
		when(ingredientService.getIngredientById(eq(99L)))
				.thenThrow(new ResourceNotFoundException("Ingredient not found: 99"));

		mockMvc.perform(get("/api/admin/inventory/ingredients/99"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.status").value(404));
	}

	@Test
	@WithMockUser(roles = "ADMIN")
	void duplicateReturns409() throws Exception {
		when(ingredientService.createIngredient(any()))
				.thenThrow(new BusinessRuleException("An ingredient with this name already exists"));

		mockMvc.perform(post("/api/admin/inventory/ingredients")
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "name": "Tomato",
								  "unit": "GRAM",
								  "stockQuantity": 1000,
								  "minimumStockLevel": 100
								}
								"""))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.status").value(409));
	}
}
