package bg.martinandonov.restaurant.demo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import bg.martinandonov.restaurant.diningtable.entity.DiningTable;
import bg.martinandonov.restaurant.diningtable.repository.DiningTableRepository;
import bg.martinandonov.restaurant.inventory.entity.Ingredient;
import bg.martinandonov.restaurant.inventory.entity.IngredientUnit;
import bg.martinandonov.restaurant.inventory.entity.RecipeIngredient;
import bg.martinandonov.restaurant.inventory.repository.IngredientRepository;
import bg.martinandonov.restaurant.inventory.repository.RecipeIngredientRepository;
import bg.martinandonov.restaurant.menu.entity.MenuCategory;
import bg.martinandonov.restaurant.menu.entity.MenuItem;
import bg.martinandonov.restaurant.menu.repository.MenuCategoryRepository;
import bg.martinandonov.restaurant.menu.repository.MenuItemRepository;
import bg.martinandonov.restaurant.menu.service.MenuAvailabilityService;
import bg.martinandonov.restaurant.reservation.entity.Reservation;
import bg.martinandonov.restaurant.reservation.repository.ReservationRepository;
import bg.martinandonov.restaurant.user.entity.AppUser;
import bg.martinandonov.restaurant.user.entity.Role;
import bg.martinandonov.restaurant.user.entity.RoleName;
import bg.martinandonov.restaurant.user.repository.AppUserRepository;
import bg.martinandonov.restaurant.user.repository.RoleRepository;

@ExtendWith(MockitoExtension.class)
class DemoDataInitializerTest {

	@Mock private Environment environment;
	@Mock private PasswordEncoder passwordEncoder;
	@Mock private RoleRepository roleRepository;
	@Mock private AppUserRepository appUserRepository;
	@Mock private DiningTableRepository diningTableRepository;
	@Mock private MenuCategoryRepository menuCategoryRepository;
	@Mock private MenuItemRepository menuItemRepository;
	@Mock private IngredientRepository ingredientRepository;
	@Mock private RecipeIngredientRepository recipeIngredientRepository;
	@Mock private MenuAvailabilityService menuAvailabilityService;
	@Mock private ReservationRepository reservationRepository;

	private DemoDataInitializer initializer;

	@BeforeEach
	void setUp() {
		Clock clock = Clock.fixed(Instant.parse("2026-08-07T12:00:00Z"), ZoneId.of("Europe/Sofia"));
		initializer = new DemoDataInitializer(
				environment,
				passwordEncoder,
				roleRepository,
				appUserRepository,
				diningTableRepository,
				menuCategoryRepository,
				menuItemRepository,
				ingredientRepository,
				recipeIngredientRepository,
				menuAvailabilityService,
				reservationRepository,
				clock);
		stubRolesLenient();
	}

	@Test
	void missingPasswordDoesNotCreateDemoUsers() {
		when(environment.getProperty("DEMO_USER_PASSWORD")).thenReturn(null);
		stubCatalogAlreadyPresent();

		initializer.run(new DefaultApplicationArguments());

		verify(appUserRepository, never()).save(any(AppUser.class));
		verify(passwordEncoder, never()).encode(anyString());
		verify(menuAvailabilityService).recalculateAllMenuItems();
	}

	@Test
	void blankPasswordDoesNotCreateDemoUsers() {
		when(environment.getProperty("DEMO_USER_PASSWORD")).thenReturn("   ");
		stubCatalogAlreadyPresent();

		initializer.run(new DefaultApplicationArguments());

		verify(appUserRepository, never()).save(any(AppUser.class));
	}

	@Test
	void configuredPasswordCreatesUsersWithBcryptAndCorrectRoles() {
		when(environment.getProperty("DEMO_USER_PASSWORD")).thenReturn("demo-pass-123");
		when(passwordEncoder.encode("demo-pass-123")).thenReturn("$2a$10$demoencodedhashvalueXXXXXXXXXXXX");
		when(appUserRepository.existsByEmail(anyString())).thenReturn(false);
		when(appUserRepository.save(any(AppUser.class))).thenAnswer(inv -> inv.getArgument(0));
		stubCatalogAlreadyPresent();
		when(appUserRepository.findByEmail(DemoDataInitializer.DEMO_CLIENT_EMAIL))
				.thenReturn(Optional.of(demoClient()));
		when(diningTableRepository.findByTableNumber(903)).thenReturn(Optional.of(demoTable()));
		when(reservationRepository.findByReservationNumber(DemoDataInitializer.DEMO_RESERVATION_NUMBER))
				.thenReturn(Optional.empty());
		when(reservationRepository.existsConfirmedConflict(any(), any(), any())).thenReturn(false);

		initializer.run(new DefaultApplicationArguments());

		verify(passwordEncoder).encode("demo-pass-123");
		ArgumentCaptor<AppUser> userCaptor = ArgumentCaptor.forClass(AppUser.class);
		verify(appUserRepository, times(4)).save(userCaptor.capture());
		assertThat(userCaptor.getAllValues()).extracting(AppUser::getEmail)
				.containsExactlyInAnyOrder(
						DemoDataInitializer.DEMO_ADMIN_EMAIL,
						DemoDataInitializer.DEMO_WAITER_EMAIL,
						DemoDataInitializer.DEMO_COOK_EMAIL,
						DemoDataInitializer.DEMO_CLIENT_EMAIL);
		assertThat(userCaptor.getAllValues()).allSatisfy(user -> {
			assertThat(user.getPassword()).startsWith("$2a$");
			assertThat(user.getPassword()).isNotEqualTo("demo-pass-123");
			assertThat(user.isEnabled()).isTrue();
		});
		assertThat(userCaptor.getAllValues().stream()
				.filter(u -> u.getEmail().equals(DemoDataInitializer.DEMO_ADMIN_EMAIL))
				.findFirst().orElseThrow().getRoles())
				.extracting(Role::getName)
				.containsExactly(RoleName.ADMIN);
	}

	@Test
	void secondRunIsIdempotentForUsersTablesMenuAndRecipes() {
		when(environment.getProperty("DEMO_USER_PASSWORD")).thenReturn("demo-pass-123");
		when(passwordEncoder.encode(anyString())).thenReturn("$2a$10$demoencodedhashvalueXXXXXXXXXXXX");
		when(appUserRepository.existsByEmail(anyString())).thenReturn(true);
		when(diningTableRepository.existsByTableNumber(any())).thenReturn(true);
		stubCatalogAlreadyPresent();
		when(reservationRepository.findByReservationNumber(DemoDataInitializer.DEMO_RESERVATION_NUMBER))
				.thenReturn(Optional.of(org.mockito.Mockito.mock(Reservation.class)));

		initializer.run(new DefaultApplicationArguments());
		initializer.run(new DefaultApplicationArguments());

		verify(appUserRepository, never()).save(any(AppUser.class));
		verify(diningTableRepository, never()).save(any(DiningTable.class));
		verify(menuCategoryRepository, never()).save(any(MenuCategory.class));
		verify(menuItemRepository, never()).save(any(MenuItem.class));
		verify(ingredientRepository, never()).save(any(Ingredient.class));
		verify(recipeIngredientRepository, never()).save(any());
		verify(reservationRepository, never()).save(any(Reservation.class));
		verify(menuAvailabilityService, times(2)).recalculateAllMenuItems();
	}

	@Test
	void createsCatalogWhenMissingAndDoesNotSeedPayments() {
		when(environment.getProperty("DEMO_USER_PASSWORD")).thenReturn(null);
		when(diningTableRepository.existsByTableNumber(any())).thenReturn(false);
		AtomicLong ids = new AtomicLong(1);
		when(diningTableRepository.save(any(DiningTable.class))).thenAnswer(inv -> {
			DiningTable t = inv.getArgument(0);
			ReflectionTestUtils.setField(t, "id", ids.getAndIncrement());
			return t;
		});
		when(menuCategoryRepository.findByNameIgnoreCase(anyString())).thenReturn(Optional.empty());
		when(menuCategoryRepository.save(any(MenuCategory.class))).thenAnswer(inv -> {
			MenuCategory c = inv.getArgument(0);
			ReflectionTestUtils.setField(c, "id", ids.getAndIncrement());
			return c;
		});
		when(ingredientRepository.findByNameIgnoreCase(anyString())).thenReturn(Optional.empty());
		when(ingredientRepository.save(any(Ingredient.class))).thenAnswer(inv -> {
			Ingredient i = inv.getArgument(0);
			ReflectionTestUtils.setField(i, "id", ids.getAndIncrement());
			return i;
		});
		when(menuItemRepository.findByCategoryIdAndNameIgnoreCase(any(), anyString())).thenReturn(Optional.empty());
		when(menuItemRepository.save(any(MenuItem.class))).thenAnswer(inv -> {
			MenuItem item = inv.getArgument(0);
			ReflectionTestUtils.setField(item, "id", ids.getAndIncrement());
			return item;
		});
		when(recipeIngredientRepository.findByMenuItemIdOrderByIngredientNameAsc(any()))
				.thenReturn(Collections.emptyList());
		when(recipeIngredientRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
		when(reservationRepository.findByReservationNumber(anyString())).thenReturn(Optional.empty());
		when(appUserRepository.findByEmail(anyString())).thenReturn(Optional.empty());

		initializer.run(new DefaultApplicationArguments());

		verify(diningTableRepository, atLeastOnce()).save(any(DiningTable.class));
		verify(menuCategoryRepository, atLeastOnce()).save(any(MenuCategory.class));
		verify(menuItemRepository, atLeastOnce()).save(any(MenuItem.class));
		verify(ingredientRepository, atLeastOnce()).save(any(Ingredient.class));
		verify(recipeIngredientRepository, atLeastOnce()).save(any());
		verify(menuAvailabilityService).recalculateAllMenuItems();
	}

	@Test
	void profileAnnotationIsDemoOnly() {
		Profile profile = DemoDataInitializer.class.getAnnotation(Profile.class);
		assertThat(profile).isNotNull();
		assertThat(profile.value()).containsExactly("demo");
	}

	private void stubRolesLenient() {
		for (RoleName roleName : RoleName.values()) {
			Role role = new Role(roleName);
			ReflectionTestUtils.setField(role, "id", (long) roleName.ordinal() + 1);
			lenient().when(roleRepository.findByName(roleName)).thenReturn(Optional.of(role));
		}
	}

	private void stubCatalogAlreadyPresent() {
		when(diningTableRepository.existsByTableNumber(any())).thenReturn(true);
		MenuCategory category = new MenuCategory(DemoDataInitializer.DEMO_PREFIX + "Starters", "d", true);
		ReflectionTestUtils.setField(category, "id", 1L);
		when(menuCategoryRepository.findByNameIgnoreCase(anyString())).thenAnswer(inv -> {
			String name = inv.getArgument(0);
			MenuCategory c = new MenuCategory(name, "d", true);
			ReflectionTestUtils.setField(c, "id", Math.abs((long) name.hashCode()));
			return Optional.of(c);
		});
		when(ingredientRepository.findByNameIgnoreCase(anyString())).thenAnswer(inv -> {
			String name = inv.getArgument(0);
			Ingredient ingredient = new Ingredient(name, IngredientUnit.GRAM, new BigDecimal("1000"), new BigDecimal("10"), true);
			ReflectionTestUtils.setField(ingredient, "id", Math.abs((long) name.hashCode()));
			return Optional.of(ingredient);
		});
		when(menuItemRepository.findByCategoryIdAndNameIgnoreCase(any(), anyString())).thenAnswer(inv -> {
			String name = inv.getArgument(1);
			MenuItem item = new MenuItem(name, "d", new BigDecimal("8.50"), true, true, category);
			ReflectionTestUtils.setField(item, "id", Math.abs((long) name.hashCode()));
			return Optional.of(item);
		});
		when(recipeIngredientRepository.findByMenuItemIdOrderByIngredientNameAsc(any()))
				.thenReturn(List.of(org.mockito.Mockito.mock(RecipeIngredient.class)));
		when(reservationRepository.findByReservationNumber(anyString()))
				.thenReturn(Optional.of(org.mockito.Mockito.mock(Reservation.class)));
	}

	private AppUser demoClient() {
		AppUser user = new AppUser(DemoDataInitializer.DEMO_CLIENT_EMAIL, "hash", "Demo Client", true);
		ReflectionTestUtils.setField(user, "id", 4L);
		return user;
	}

	private DiningTable demoTable() {
		DiningTable table = new DiningTable(903, "DEMO Table 903", 4);
		ReflectionTestUtils.setField(table, "id", 3L);
		return table;
	}
}