package bg.martinandonov.restaurant;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import bg.martinandonov.restaurant.diningtable.repository.DiningTableRepository;
import bg.martinandonov.restaurant.inventory.repository.IngredientRepository;
import bg.martinandonov.restaurant.inventory.repository.RecipeIngredientRepository;
import bg.martinandonov.restaurant.menu.repository.MenuCategoryRepository;
import bg.martinandonov.restaurant.menu.repository.MenuItemRepository;
import bg.martinandonov.restaurant.order.repository.OrderItemRepository;
import bg.martinandonov.restaurant.order.repository.RestaurantOrderRepository;
import bg.martinandonov.restaurant.user.repository.AppUserRepository;
import bg.martinandonov.restaurant.user.repository.RoleRepository;

@SpringBootTest
class RestaurantPosReservationsApplicationTests {

	@MockitoBean
	private AppUserRepository appUserRepository;

	@MockitoBean
	private RoleRepository roleRepository;

	@MockitoBean
	private MenuCategoryRepository menuCategoryRepository;

	@MockitoBean
	private MenuItemRepository menuItemRepository;

	@MockitoBean
	private IngredientRepository ingredientRepository;

	@MockitoBean
	private RecipeIngredientRepository recipeIngredientRepository;

	@MockitoBean
	private DiningTableRepository diningTableRepository;

	@MockitoBean
	private RestaurantOrderRepository restaurantOrderRepository;

	@MockitoBean
	private OrderItemRepository orderItemRepository;

	@Test
	void contextLoads() {
	}

}
