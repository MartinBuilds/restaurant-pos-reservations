package bg.martinandonov.restaurant.menu.controller;

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
import bg.martinandonov.restaurant.menu.dto.MenuCategoryResponse;
import bg.martinandonov.restaurant.menu.dto.MenuItemResponse;
import bg.martinandonov.restaurant.menu.service.MenuCategoryService;
import bg.martinandonov.restaurant.menu.service.MenuItemService;
import bg.martinandonov.restaurant.security.SecurityConfig;

@WebMvcTest(controllers = {
		AdminMenuCategoryController.class,
		AdminMenuItemController.class,
		PublicMenuController.class
})
@Import({ SecurityConfig.class, GlobalExceptionHandler.class })
class MenuControllerSecurityTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private MenuCategoryService menuCategoryService;

	@MockitoBean
	private MenuItemService menuItemService;

	@MockitoBean
	private UserDetailsService userDetailsService;

	@Test
	@WithAnonymousUser
	void anonymousCannotAccessAdminCategories() throws Exception {
		mockMvc.perform(get("/api/admin/menu/categories"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	@WithMockUser(roles = "WAITER")
	void waiterCannotAccessAdminCategories() throws Exception {
		mockMvc.perform(get("/api/admin/menu/categories"))
				.andExpect(status().isForbidden());
	}

	@Test
	@WithMockUser(roles = "ADMIN")
	void adminCanAccessAdminCategories() throws Exception {
		when(menuCategoryService.getAllCategories()).thenReturn(List.of(
				new MenuCategoryResponse(1L, "Salads", null, true)));

		mockMvc.perform(get("/api/admin/menu/categories"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].name").value("Salads"));
	}

	@Test
	@WithAnonymousUser
	void publicMenuIsAccessibleWithoutLogin() throws Exception {
		when(menuItemService.getPublicMenu()).thenReturn(List.of(
				new MenuItemResponse(
						1L,
						"Lemonade",
						null,
						new BigDecimal("3.50"),
						true,
						true,
						2L,
						"Drinks")));

		mockMvc.perform(get("/api/public/menu"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].name").value("Lemonade"));
	}

	@Test
	@WithMockUser(roles = "ADMIN")
	void createCategoryReturns201() throws Exception {
		when(menuCategoryService.createCategory(any()))
				.thenReturn(new MenuCategoryResponse(1L, "Salads", null, true));

		mockMvc.perform(post("/api/admin/menu/categories")
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "name": "Salads",
								  "description": "Fresh salads"
								}
								"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.name").value("Salads"));
	}

	@Test
	@WithMockUser(roles = "ADMIN")
	void invalidInputReturns400() throws Exception {
		when(menuItemService.createMenuItem(any()))
				.thenThrow(new InvalidRequestException("Price must be greater than 0"));

		mockMvc.perform(post("/api/admin/menu/items")
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "name": "Soup",
								  "price": 0,
								  "categoryId": 1
								}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Price must be greater than 0"));
	}

	@Test
	@WithMockUser(roles = "ADMIN")
	void missingResourceReturns404() throws Exception {
		when(menuCategoryService.getCategoryById(eq(99L)))
				.thenThrow(new ResourceNotFoundException("Menu category not found: 99"));

		mockMvc.perform(get("/api/admin/menu/categories/99"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.status").value(404));
	}

	@Test
	@WithMockUser(roles = "ADMIN")
	void duplicateReturns409() throws Exception {
		when(menuCategoryService.createCategory(any()))
				.thenThrow(new BusinessRuleException("A menu category with this name already exists"));

		mockMvc.perform(post("/api/admin/menu/categories")
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "name": "Salads"
								}
								"""))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.status").value(409));
	}
}
