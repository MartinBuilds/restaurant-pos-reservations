package bg.martinandonov.restaurant.client;

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
class ClientUiHttpResourceSecurityTest {

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
	@WithMockUser(roles = "CLIENT")
	void clientRootRedirectsToClient() throws Exception {
		mockMvc.perform(get("/"))
				.andExpect(status().isFound())
				.andExpect(redirectedUrl("/client"));
	}

	@Test
	@WithMockUser(roles = "CLIENT")
	void clientCanLoadIndexCssAndModules() throws Exception {
		mockMvc.perform(get("/client/index.html"))
				.andExpect(status().isOk())
				.andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML));
		mockMvc.perform(get("/client/css/client.css")).andExpect(status().isOk());
		mockMvc.perform(get("/client/js/app.js")).andExpect(status().isOk());
		mockMvc.perform(get("/client/js/api.js")).andExpect(status().isOk());
		mockMvc.perform(get("/client/js/router.js")).andExpect(status().isOk());
		mockMvc.perform(get("/client/js/views/availability.js")).andExpect(status().isOk());
		mockMvc.perform(get("/client/js/views/reservations.js")).andExpect(status().isOk());
		mockMvc.perform(get("/client/js/views/reservation-details.js")).andExpect(status().isOk());
		mockMvc.perform(get("/client/js/views/reservation-form.js")).andExpect(status().isOk());
	}

	@Test
	@WithMockUser(roles = "CLIENT")
	void clientDeniedOtherUisAndOperations() throws Exception {
		mockMvc.perform(get("/admin/index.html")).andExpect(status().isForbidden());
		mockMvc.perform(get("/waiter/index.html")).andExpect(status().isForbidden());
		mockMvc.perform(get("/kitchen/index.html")).andExpect(status().isForbidden());
		mockMvc.perform(get("/operations/js/api.js")).andExpect(status().isForbidden());
	}

	@Test
	@WithMockUser(roles = "ADMIN")
	void adminWithoutClientDeniedClientAssets() throws Exception {
		mockMvc.perform(get("/client/index.html")).andExpect(status().isForbidden());
		mockMvc.perform(get("/client/js/app.js")).andExpect(status().isForbidden());
	}

	@Test
	@WithMockUser(roles = "WAITER")
	void waiterDeniedClientAssets() throws Exception {
		mockMvc.perform(get("/client/index.html")).andExpect(status().isForbidden());
	}

	@Test
	@WithMockUser(roles = "COOK")
	void cookDeniedClientAssets() throws Exception {
		mockMvc.perform(get("/client/css/client.css")).andExpect(status().isForbidden());
	}

	@Test
	@WithMockUser(roles = "ADMIN")
	void adminStillAllowedAdminUi() throws Exception {
		mockMvc.perform(get("/admin/index.html")).andExpect(status().isOk());
	}

	@Test
	@WithMockUser(roles = "WAITER")
	void waiterStillAllowedWaiterUi() throws Exception {
		mockMvc.perform(get("/waiter/index.html")).andExpect(status().isOk());
	}

	@Test
	@WithMockUser(roles = "COOK")
	void cookStillAllowedKitchenUi() throws Exception {
		mockMvc.perform(get("/kitchen/index.html")).andExpect(status().isOk());
	}
}