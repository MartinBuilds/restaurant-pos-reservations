package bg.martinandonov.restaurant.admin;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import bg.martinandonov.restaurant.diningtable.repository.DiningTableRepository;
import bg.martinandonov.restaurant.inventory.repository.IngredientRepository;
import bg.martinandonov.restaurant.inventory.repository.RecipeIngredientRepository;
import bg.martinandonov.restaurant.menu.repository.MenuCategoryRepository;
import bg.martinandonov.restaurant.menu.repository.MenuItemRepository;
import bg.martinandonov.restaurant.order.repository.OrderItemRepository;
import bg.martinandonov.restaurant.order.repository.RestaurantOrderRepository;
import bg.martinandonov.restaurant.payment.repository.PaymentRepository;
import bg.martinandonov.restaurant.report.repository.SalesReportRepository;
import bg.martinandonov.restaurant.reservation.repository.ReservationRepository;
import bg.martinandonov.restaurant.user.repository.AppUserRepository;
import bg.martinandonov.restaurant.user.repository.RoleRepository;

@SpringBootTest
@AutoConfigureMockMvc
class AdminUiHttpResourceSecurityTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean private AppUserRepository appUserRepository;
	@MockitoBean private RoleRepository roleRepository;
	@MockitoBean private MenuCategoryRepository menuCategoryRepository;
	@MockitoBean private MenuItemRepository menuItemRepository;
	@MockitoBean private IngredientRepository ingredientRepository;
	@MockitoBean private RecipeIngredientRepository recipeIngredientRepository;
	@MockitoBean private DiningTableRepository diningTableRepository;
	@MockitoBean private RestaurantOrderRepository restaurantOrderRepository;
	@MockitoBean private OrderItemRepository orderItemRepository;
	@MockitoBean private PaymentRepository paymentRepository;
	@MockitoBean private SalesReportRepository salesReportRepository;
	@MockitoBean private ReservationRepository reservationRepository;

	@Test
	void rootRedirectsToLogin() throws Exception {
		mockMvc.perform(get("/"))
				.andExpect(status().isFound())
				.andExpect(redirectedUrl("/login"));
	}

	@Test
	@WithMockUser(roles = "ADMIN")
	void authenticatedAdminRootRedirectsToAdmin() throws Exception {
		mockMvc.perform(get("/"))
				.andExpect(status().isFound())
				.andExpect(redirectedUrl("/admin"));
	}

	@Test
	@WithMockUser(roles = "ADMIN")
	void adminCanLoadIndexCssAndAppModule() throws Exception {
		mockMvc.perform(get("/admin/index.html"))
				.andExpect(status().isOk())
				.andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML));
		mockMvc.perform(get("/admin/css/admin.css"))
				.andExpect(status().isOk());
		mockMvc.perform(get("/admin/js/app.js"))
				.andExpect(status().isOk());
	}

	@Test
	@WithMockUser(roles = "WAITER")
	void waiterCannotLoadAdminIndex() throws Exception {
		mockMvc.perform(get("/admin/index.html")).andExpect(status().isForbidden());
	}

	@Test
	@WithMockUser(roles = "CLIENT")
	void clientCannotLoadAdminJs() throws Exception {
		mockMvc.perform(get("/admin/js/app.js")).andExpect(status().isForbidden());
	}
}