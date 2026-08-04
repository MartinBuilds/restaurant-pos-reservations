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
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import bg.martinandonov.restaurant.common.exception.BusinessRuleException;
import bg.martinandonov.restaurant.common.exception.InvalidRequestException;
import bg.martinandonov.restaurant.common.exception.ResourceNotFoundException;
import bg.martinandonov.restaurant.menu.dto.CreateMenuItemRequest;
import bg.martinandonov.restaurant.menu.dto.MenuItemResponse;
import bg.martinandonov.restaurant.menu.entity.MenuCategory;
import bg.martinandonov.restaurant.menu.entity.MenuItem;
import bg.martinandonov.restaurant.menu.repository.MenuItemRepository;

@ExtendWith(MockitoExtension.class)
class MenuItemServiceTest {

	@Mock
	private MenuItemRepository menuItemRepository;

	@Mock
	private MenuCategoryService menuCategoryService;

	@InjectMocks
	private MenuItemService menuItemService;

	private MenuCategory salads;
	private MenuCategory desserts;

	@BeforeEach
	void setUp() {
		salads = new MenuCategory("Salads", null, true);
		ReflectionTestUtils.setField(salads, "id", 1L);
		desserts = new MenuCategory("Desserts", null, true);
		ReflectionTestUtils.setField(desserts, "id", 2L);
	}

	@Test
	void createMenuItemUsesBigDecimalPrice() {
		CreateMenuItemRequest request = new CreateMenuItemRequest();
		request.setName("Caesar Salad");
		request.setPrice(new BigDecimal("12.50"));
		request.setCategoryId(1L);

		when(menuCategoryService.getCategoryEntity(1L)).thenReturn(salads);
		when(menuItemRepository.existsByCategoryIdAndNameIgnoreCase(1L, "Caesar Salad")).thenReturn(false);
		when(menuItemRepository.save(any(MenuItem.class))).thenAnswer(invocation -> {
			MenuItem item = invocation.getArgument(0);
			ReflectionTestUtils.setField(item, "id", 10L);
			return item;
		});

		MenuItemResponse response = menuItemService.createMenuItem(request);

		ArgumentCaptor<MenuItem> captor = ArgumentCaptor.forClass(MenuItem.class);
		verify(menuItemRepository).save(captor.capture());
		assertThat(captor.getValue().getPrice()).isEqualByComparingTo("12.50");
		assertThat(response.getPrice()).isEqualByComparingTo("12.50");
		assertThat(response.getCategoryId()).isEqualTo(1L);
	}

	@Test
	void createMenuItemRejectsNonPositivePrice() {
		CreateMenuItemRequest request = new CreateMenuItemRequest();
		request.setName("Soup");
		request.setPrice(BigDecimal.ZERO);
		request.setCategoryId(1L);

		assertThatThrownBy(() -> menuItemService.createMenuItem(request))
				.isInstanceOf(InvalidRequestException.class)
				.hasMessageContaining("greater than 0");
		verify(menuItemRepository, never()).save(any());
	}

	@Test
	void createMenuItemRejectsMissingCategory() {
		CreateMenuItemRequest request = new CreateMenuItemRequest();
		request.setName("Soup");
		request.setPrice(new BigDecimal("5.00"));
		request.setCategoryId(99L);

		when(menuCategoryService.getCategoryEntity(99L))
				.thenThrow(new ResourceNotFoundException("Menu category not found: 99"));

		assertThatThrownBy(() -> menuItemService.createMenuItem(request))
				.isInstanceOf(ResourceNotFoundException.class)
				.hasMessageContaining("99");
	}

	@Test
	void createMenuItemRejectsDuplicateInSameCategory() {
		CreateMenuItemRequest request = new CreateMenuItemRequest();
		request.setName("Caesar Salad");
		request.setPrice(new BigDecimal("12.50"));
		request.setCategoryId(1L);

		when(menuCategoryService.getCategoryEntity(1L)).thenReturn(salads);
		when(menuItemRepository.existsByCategoryIdAndNameIgnoreCase(1L, "Caesar Salad")).thenReturn(true);

		assertThatThrownBy(() -> menuItemService.createMenuItem(request))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("already exists");
	}

	@Test
	void sameItemNameAllowedInDifferentCategories() {
		CreateMenuItemRequest request = new CreateMenuItemRequest();
		request.setName("Special");
		request.setPrice(new BigDecimal("9.99"));
		request.setCategoryId(2L);

		when(menuCategoryService.getCategoryEntity(2L)).thenReturn(desserts);
		when(menuItemRepository.existsByCategoryIdAndNameIgnoreCase(2L, "Special")).thenReturn(false);
		when(menuItemRepository.save(any(MenuItem.class))).thenAnswer(invocation -> {
			MenuItem item = invocation.getArgument(0);
			ReflectionTestUtils.setField(item, "id", 11L);
			return item;
		});

		MenuItemResponse response = menuItemService.createMenuItem(request);

		assertThat(response.getName()).isEqualTo("Special");
		assertThat(response.getCategoryId()).isEqualTo(2L);
		verify(menuItemRepository).existsByCategoryIdAndNameIgnoreCase(2L, "Special");
	}

	@Test
	void inactiveCategoryRejectsActiveItem() {
		MenuCategory inactive = new MenuCategory("Archived", null, false);
		ReflectionTestUtils.setField(inactive, "id", 3L);

		CreateMenuItemRequest request = new CreateMenuItemRequest();
		request.setName("Old Dish");
		request.setPrice(new BigDecimal("8.00"));
		request.setCategoryId(3L);

		when(menuCategoryService.getCategoryEntity(3L)).thenReturn(inactive);

		assertThatThrownBy(() -> menuItemService.createMenuItem(request))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("inactive category");
		verify(menuItemRepository, never()).save(any());
	}

	@Test
	void publicMenuContainsOnlyActiveAvailableItemsInActiveCategories() {
		MenuItem publicItem = new MenuItem(
				"Lemonade",
				null,
				new BigDecimal("3.50"),
				true,
				true,
				salads);
		ReflectionTestUtils.setField(publicItem, "id", 20L);

		when(menuItemRepository.findByActiveTrueAndAvailableTrueAndCategory_ActiveTrueOrderByNameAsc())
				.thenReturn(List.of(publicItem));

		List<MenuItemResponse> menu = menuItemService.getPublicMenu();

		assertThat(menu).hasSize(1);
		assertThat(menu.get(0).isActive()).isTrue();
		assertThat(menu.get(0).isAvailable()).isTrue();
		assertThat(menu.get(0).getCategoryName()).isEqualTo("Salads");
	}
}
