package bg.martinandonov.restaurant;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import bg.martinandonov.restaurant.menu.repository.MenuCategoryRepository;
import bg.martinandonov.restaurant.menu.repository.MenuItemRepository;
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

	@Test
	void contextLoads() {
	}

}
