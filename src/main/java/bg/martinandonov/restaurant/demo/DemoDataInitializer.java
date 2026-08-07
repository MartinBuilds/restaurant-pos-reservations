package bg.martinandonov.restaurant.demo;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

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
import bg.martinandonov.restaurant.user.EmailNormalizer;
import bg.martinandonov.restaurant.user.entity.AppUser;
import bg.martinandonov.restaurant.user.entity.Role;
import bg.martinandonov.restaurant.user.entity.RoleName;
import bg.martinandonov.restaurant.user.repository.AppUserRepository;
import bg.martinandonov.restaurant.user.repository.RoleRepository;

/**
 * Optional demo dataset for local presentation. Active only with {@code demo} profile.
 * Does not delete or reset existing data. Creates DEMO-prefixed records only when missing.
 */
@Component
@Profile("demo")
@Order(100)
public class DemoDataInitializer implements ApplicationRunner {

	static final String DEMO_ADMIN_EMAIL = "demo.admin@example.com";
	static final String DEMO_WAITER_EMAIL = "demo.waiter@example.com";
	static final String DEMO_COOK_EMAIL = "demo.cook@example.com";
	static final String DEMO_CLIENT_EMAIL = "demo.client@example.com";
	static final String DEMO_RESERVATION_NUMBER = "DEMO-RESERVATION-SEED-001";
	static final String DEMO_PREFIX = "DEMO ";

	private static final Logger log = LoggerFactory.getLogger(DemoDataInitializer.class);
	private static final int MIN_PASSWORD_LENGTH = 8;

	private final Environment environment;
	private final PasswordEncoder passwordEncoder;
	private final RoleRepository roleRepository;
	private final AppUserRepository appUserRepository;
	private final DiningTableRepository diningTableRepository;
	private final MenuCategoryRepository menuCategoryRepository;
	private final MenuItemRepository menuItemRepository;
	private final IngredientRepository ingredientRepository;
	private final RecipeIngredientRepository recipeIngredientRepository;
	private final MenuAvailabilityService menuAvailabilityService;
	private final ReservationRepository reservationRepository;
	private final Clock clock;

	public DemoDataInitializer(
			Environment environment,
			PasswordEncoder passwordEncoder,
			RoleRepository roleRepository,
			AppUserRepository appUserRepository,
			DiningTableRepository diningTableRepository,
			MenuCategoryRepository menuCategoryRepository,
			MenuItemRepository menuItemRepository,
			IngredientRepository ingredientRepository,
			RecipeIngredientRepository recipeIngredientRepository,
			MenuAvailabilityService menuAvailabilityService,
			ReservationRepository reservationRepository,
			Clock clock) {
		this.environment = environment;
		this.passwordEncoder = passwordEncoder;
		this.roleRepository = roleRepository;
		this.appUserRepository = appUserRepository;
		this.diningTableRepository = diningTableRepository;
		this.menuCategoryRepository = menuCategoryRepository;
		this.menuItemRepository = menuItemRepository;
		this.ingredientRepository = ingredientRepository;
		this.recipeIngredientRepository = recipeIngredientRepository;
		this.menuAvailabilityService = menuAvailabilityService;
		this.reservationRepository = reservationRepository;
		this.clock = clock;
	}

	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		ensureRoles();
		seedDemoUsersIfPasswordConfigured();
		seedDiningTables();
		Map<String, MenuCategory> categories = seedCategories();
		Map<String, Ingredient> ingredients = seedIngredients();
		Map<String, MenuItem> items = seedMenuItems(categories);
		seedRecipes(items, ingredients);
		menuAvailabilityService.recalculateAllMenuItems();
		seedDemoReservationIfPossible();
		log.info("Demo data initializer completed");
	}

	private void ensureRoles() {
		for (RoleName roleName : RoleName.values()) {
			roleRepository.findByName(roleName).orElseGet(() -> roleRepository.save(new Role(roleName)));
		}
	}

	private void seedDemoUsersIfPasswordConfigured() {
		String rawPassword = environment.getProperty("DEMO_USER_PASSWORD");
		if (rawPassword == null || rawPassword.isBlank()) {
			log.warn("DEMO_USER_PASSWORD is missing or blank; demo users were not created");
			return;
		}
		if (rawPassword.length() < MIN_PASSWORD_LENGTH) {
			log.warn("DEMO_USER_PASSWORD is too short; demo users were not created");
			return;
		}

		String encoded = passwordEncoder.encode(rawPassword);
		ensureUser(DEMO_ADMIN_EMAIL, "Demo Admin", EnumSet.of(RoleName.ADMIN), encoded);
		ensureUser(DEMO_WAITER_EMAIL, "Demo Waiter", EnumSet.of(RoleName.WAITER), encoded);
		ensureUser(DEMO_COOK_EMAIL, "Demo Cook", EnumSet.of(RoleName.COOK), encoded);
		ensureUser(DEMO_CLIENT_EMAIL, "Demo Client", EnumSet.of(RoleName.CLIENT), encoded);
		log.info("Demo users ensured for presentation emails");
	}

	private void ensureUser(String emailRaw, String fullName, Set<RoleName> roles, String encodedPassword) {
		String email = EmailNormalizer.normalize(emailRaw);
		if (appUserRepository.existsByEmail(email)) {
			return;
		}
		AppUser user = new AppUser(email, encodedPassword, fullName, true);
		for (RoleName roleName : roles) {
			Role role = roleRepository.findByName(roleName)
					.orElseThrow(() -> new IllegalStateException(roleName + " role is missing"));
			user.getRoles().add(role);
		}
		appUserRepository.save(user);
	}

	private void seedDiningTables() {
		int[][] tables = {
				{ 901, 2 },
				{ 902, 2 },
				{ 903, 4 },
				{ 904, 4 },
				{ 905, 6 },
				{ 906, 8 }
		};
		for (int[] row : tables) {
			int number = row[0];
			int capacity = row[1];
			if (diningTableRepository.existsByTableNumber(number)) {
				continue;
			}
			diningTableRepository.save(new DiningTable(number, DEMO_PREFIX + "Table " + number, capacity));
		}
	}

	private Map<String, MenuCategory> seedCategories() {
		Map<String, String> defs = new LinkedHashMap<>();
		defs.put(DEMO_PREFIX + "Starters", "Demo starters");
		defs.put(DEMO_PREFIX + "Mains", "Demo mains");
		defs.put(DEMO_PREFIX + "Desserts", "Demo desserts");
		defs.put(DEMO_PREFIX + "Drinks", "Demo drinks");

		Map<String, MenuCategory> result = new LinkedHashMap<>();
		for (Map.Entry<String, String> entry : defs.entrySet()) {
			MenuCategory category = menuCategoryRepository.findByNameIgnoreCase(entry.getKey())
					.orElseGet(() -> menuCategoryRepository.save(new MenuCategory(entry.getKey(), entry.getValue(), true)));
			result.put(entry.getKey(), category);
		}
		return result;
	}

	private Map<String, Ingredient> seedIngredients() {
		record Ing(String name, IngredientUnit unit, String stock, String min) {
		}
		List<Ing> defs = List.of(
				new Ing(DEMO_PREFIX + "Lettuce", IngredientUnit.GRAM, "5000", "200"),
				new Ing(DEMO_PREFIX + "Tomato", IngredientUnit.GRAM, "4000", "200"),
				new Ing(DEMO_PREFIX + "Chicken", IngredientUnit.GRAM, "8000", "500"),
				new Ing(DEMO_PREFIX + "Pasta", IngredientUnit.GRAM, "6000", "300"),
				new Ing(DEMO_PREFIX + "Beef Patty", IngredientUnit.GRAM, "5000", "300"),
				new Ing(DEMO_PREFIX + "Burger Bun", IngredientUnit.PIECE, "200", "20"),
				new Ing(DEMO_PREFIX + "Flour", IngredientUnit.GRAM, "10000", "500"),
				new Ing(DEMO_PREFIX + "Sugar", IngredientUnit.GRAM, "5000", "200"),
				new Ing(DEMO_PREFIX + "Water Bottle", IngredientUnit.PIECE, "300", "20"),
				new Ing(DEMO_PREFIX + "Lemon", IngredientUnit.PIECE, "150", "10"),
				new Ing(DEMO_PREFIX + "Vegetable Broth", IngredientUnit.MILLILITER, "10000", "500"));

		Map<String, Ingredient> result = new LinkedHashMap<>();
		for (Ing def : defs) {
			Ingredient ingredient = ingredientRepository.findByNameIgnoreCase(def.name())
					.orElseGet(() -> ingredientRepository.save(new Ingredient(
							def.name(),
							def.unit(),
							new BigDecimal(def.stock()),
							new BigDecimal(def.min()),
							true)));
			result.put(def.name(), ingredient);
		}
		return result;
	}

	private Map<String, MenuItem> seedMenuItems(Map<String, MenuCategory> categories) {
		record Item(String category, String name, String description, String price) {
		}
		List<Item> defs = List.of(
				new Item(DEMO_PREFIX + "Starters", DEMO_PREFIX + "Salad", "Fresh green salad", "8.50"),
				new Item(DEMO_PREFIX + "Starters", DEMO_PREFIX + "Soup", "Vegetable soup", "6.90"),
				new Item(DEMO_PREFIX + "Mains", DEMO_PREFIX + "Pasta", "Pasta with chicken", "14.50"),
				new Item(DEMO_PREFIX + "Mains", DEMO_PREFIX + "Burger", "Beef burger", "15.90"),
				new Item(DEMO_PREFIX + "Mains", DEMO_PREFIX + "Chicken", "Roasted chicken", "13.50"),
				new Item(DEMO_PREFIX + "Desserts", DEMO_PREFIX + "Cake", "House cake", "7.50"),
				new Item(DEMO_PREFIX + "Drinks", DEMO_PREFIX + "Water", "Bottled water", "2.50"),
				new Item(DEMO_PREFIX + "Drinks", DEMO_PREFIX + "Lemonade", "Fresh lemonade", "4.20"));

		Map<String, MenuItem> result = new LinkedHashMap<>();
		for (Item def : defs) {
			MenuCategory category = categories.get(def.category());
			MenuItem item = menuItemRepository.findByCategoryIdAndNameIgnoreCase(category.getId(), def.name())
					.orElseGet(() -> menuItemRepository.save(new MenuItem(
							def.name(),
							def.description(),
							new BigDecimal(def.price()),
							true,
							true,
							category)));
			result.put(def.name(), item);
		}
		return result;
	}

	private void seedRecipes(Map<String, MenuItem> items, Map<String, Ingredient> ingredients) {
		seedRecipeIfEmpty(items.get(DEMO_PREFIX + "Salad"), List.of(
				entry(ingredients, DEMO_PREFIX + "Lettuce", "120"),
				entry(ingredients, DEMO_PREFIX + "Tomato", "80")));
		seedRecipeIfEmpty(items.get(DEMO_PREFIX + "Soup"), List.of(
				entry(ingredients, DEMO_PREFIX + "Vegetable Broth", "300"),
				entry(ingredients, DEMO_PREFIX + "Tomato", "50")));
		seedRecipeIfEmpty(items.get(DEMO_PREFIX + "Pasta"), List.of(
				entry(ingredients, DEMO_PREFIX + "Pasta", "180"),
				entry(ingredients, DEMO_PREFIX + "Chicken", "100")));
		seedRecipeIfEmpty(items.get(DEMO_PREFIX + "Burger"), List.of(
				entry(ingredients, DEMO_PREFIX + "Beef Patty", "150"),
				entry(ingredients, DEMO_PREFIX + "Burger Bun", "1")));
		seedRecipeIfEmpty(items.get(DEMO_PREFIX + "Chicken"), List.of(
				entry(ingredients, DEMO_PREFIX + "Chicken", "220")));
		seedRecipeIfEmpty(items.get(DEMO_PREFIX + "Cake"), List.of(
				entry(ingredients, DEMO_PREFIX + "Flour", "80"),
				entry(ingredients, DEMO_PREFIX + "Sugar", "40")));
		seedRecipeIfEmpty(items.get(DEMO_PREFIX + "Water"), List.of(
				entry(ingredients, DEMO_PREFIX + "Water Bottle", "1")));
		seedRecipeIfEmpty(items.get(DEMO_PREFIX + "Lemonade"), List.of(
				entry(ingredients, DEMO_PREFIX + "Lemon", "1"),
				entry(ingredients, DEMO_PREFIX + "Sugar", "20")));
	}

	private static Map.Entry<Ingredient, String> entry(Map<String, Ingredient> ingredients, String name, String qty) {
		return new java.util.AbstractMap.SimpleImmutableEntry<>(ingredients.get(name), qty);
	}

	private void seedRecipeIfEmpty(MenuItem item, List<Map.Entry<Ingredient, String>> components) {
		if (item == null) {
			return;
		}
		List<RecipeIngredient> existing = recipeIngredientRepository.findByMenuItemIdOrderByIngredientNameAsc(item.getId());
		if (!existing.isEmpty()) {
			return;
		}
		for (Map.Entry<Ingredient, String> component : components) {
			if (component.getKey() == null) {
				continue;
			}
			recipeIngredientRepository.save(new RecipeIngredient(item, component.getKey(), new BigDecimal(component.getValue())));
		}
	}
	private void seedDemoReservationIfPossible() {
		if (reservationRepository.findByReservationNumber(DEMO_RESERVATION_NUMBER).isPresent()) {
			return;
		}
		AppUser client = appUserRepository.findByEmail(EmailNormalizer.normalize(DEMO_CLIENT_EMAIL)).orElse(null);
		DiningTable table = diningTableRepository.findByTableNumber(903).orElse(null);
		if (client == null || table == null) {
			log.info("Demo reservation skipped: demo client or table is not available");
			return;
		}
		LocalDateTime start = LocalDateTime.now(clock).plusDays(2).withHour(19).withMinute(0).withSecond(0).withNano(0);
		LocalDateTime end = start.plusHours(2);
		if (reservationRepository.existsConfirmedConflict(table.getId(), start, end)) {
			log.info("Demo reservation skipped: selected table interval already has a conflict");
			return;
		}
		LocalDateTime now = LocalDateTime.now(clock);
		reservationRepository.save(new Reservation(
				DEMO_RESERVATION_NUMBER,
				table,
				client,
				start,
				end,
				4,
				"DEMO_SEED_RESERVATION",
				now));
		log.info("Demo reservation ensured");
	}
}