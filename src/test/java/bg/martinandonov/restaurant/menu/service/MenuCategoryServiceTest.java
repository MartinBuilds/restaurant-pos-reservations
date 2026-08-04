package bg.martinandonov.restaurant.menu.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import bg.martinandonov.restaurant.common.exception.BusinessRuleException;
import bg.martinandonov.restaurant.common.exception.ResourceNotFoundException;
import bg.martinandonov.restaurant.menu.dto.CreateMenuCategoryRequest;
import bg.martinandonov.restaurant.menu.dto.MenuCategoryResponse;
import bg.martinandonov.restaurant.menu.entity.MenuCategory;
import bg.martinandonov.restaurant.menu.repository.MenuCategoryRepository;

@ExtendWith(MockitoExtension.class)
class MenuCategoryServiceTest {

	@Mock
	private MenuCategoryRepository menuCategoryRepository;

	@InjectMocks
	private MenuCategoryService menuCategoryService;

	@Test
	void createCategoryTrimsName() {
		CreateMenuCategoryRequest request = new CreateMenuCategoryRequest();
		request.setName("  Salads  ");
		request.setDescription("Fresh salads");

		when(menuCategoryRepository.existsByNameIgnoreCase("Salads")).thenReturn(false);
		when(menuCategoryRepository.save(any(MenuCategory.class))).thenAnswer(invocation -> {
			MenuCategory category = invocation.getArgument(0);
			ReflectionTestUtils.setField(category, "id", 1L);
			return category;
		});

		MenuCategoryResponse response = menuCategoryService.createCategory(request);

		ArgumentCaptor<MenuCategory> captor = ArgumentCaptor.forClass(MenuCategory.class);
		verify(menuCategoryRepository).save(captor.capture());
		assertThat(captor.getValue().getName()).isEqualTo("Salads");
		assertThat(response.getName()).isEqualTo("Salads");
		assertThat(response.isActive()).isTrue();
	}

	@Test
	void createCategoryRejectsDuplicate() {
		CreateMenuCategoryRequest request = new CreateMenuCategoryRequest();
		request.setName("Salads");

		when(menuCategoryRepository.existsByNameIgnoreCase("Salads")).thenReturn(true);

		assertThatThrownBy(() -> menuCategoryService.createCategory(request))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("already exists");
		verify(menuCategoryRepository, never()).save(any());
	}

	@Test
	void getCategoryByIdThrowsWhenMissing() {
		when(menuCategoryRepository.findById(99L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> menuCategoryService.getCategoryById(99L))
				.isInstanceOf(ResourceNotFoundException.class)
				.hasMessageContaining("99");
	}

	@Test
	void getActiveCategoriesExcludesInactive() {
		MenuCategory active = new MenuCategory("Drinks", null, true);
		ReflectionTestUtils.setField(active, "id", 1L);
		when(menuCategoryRepository.findByActiveTrueOrderByNameAsc()).thenReturn(List.of(active));

		List<MenuCategoryResponse> categories = menuCategoryService.getActiveCategories();

		assertThat(categories).hasSize(1);
		assertThat(categories.get(0).getName()).isEqualTo("Drinks");
		assertThat(categories.get(0).isActive()).isTrue();
	}
}
