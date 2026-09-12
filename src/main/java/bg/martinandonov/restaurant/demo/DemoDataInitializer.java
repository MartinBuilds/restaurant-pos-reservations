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
 * Does not delete existing data. Creates realistic sample records only when missing.
 */
@Component
@Profile("demo")
@Order(100)
public class DemoDataInitializer implements ApplicationRunner {

	static final String SEED_ADMIN_EMAIL = "maria.adminova@example.com";
	static final String SEED_WAITER_EMAIL = "georgi.stoyanov@example.com";
	static final String SEED_COOK_EMAIL = "ivan.petkov@example.com";
	static final String SEED_CLIENT_EMAIL = "elena.dimitrova@example.com";
	static final String SEED_RESERVATION_NUMBER = "RES-20260809-1001";

	static final String CAT_STARTERS = "Предястия";
	static final String CAT_MAINS = "Основни ястия";
	static final String CAT_DESSERTS = "Десерти";
	static final String CAT_DRINKS = "Напитки";

	static final String ITEM_SALAD = "Шопска салата";
	static final String ITEM_SOUP = "Пилешка супа";
	static final String ITEM_PASTA = "Паста с пиле";
	static final String ITEM_BURGER = "Телешки бургер";
	static final String ITEM_CHICKEN = "Печено пиле";
	static final String ITEM_CAKE = "Домашна торта";
	static final String ITEM_WATER = "Минерална вода";
	static final String ITEM_LEMONADE = "Домашна лимонада";

	private static final Logger log = LoggerFactory.getLogger(DemoDataInitializer.class);
	private static final int MIN_PASSWORD_LENGTH = 8;
	private static final int SEED_TABLE_FOR_RESERVATION = 3;

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
		ensureUser(SEED_ADMIN_EMAIL, "Мария Админова", EnumSet.of(RoleName.ADMIN), encoded);
		ensureUser(SEED_WAITER_EMAIL, "Георги Стоянов", EnumSet.of(RoleName.WAITER), encoded);
		ensureUser(SEED_COOK_EMAIL, "Иван Петков", EnumSet.of(RoleName.COOK), encoded);
		ensureUser(SEED_CLIENT_EMAIL, "Елена Димитрова", EnumSet.of(RoleName.CLIENT), encoded);
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
		record TableDef(int number, String name, int capacity) {
		}
		List<TableDef> tables = List.of(
				new TableDef(1, "Прозорец 1", 2),
				new TableDef(2, "Прозорец 2", 2),
				new TableDef(3, "Салон 3", 4),
				new TableDef(4, "Салон 4", 4),
				new TableDef(5, "Тераса 5", 6),
				new TableDef(6, "VIP 6", 8));
		for (TableDef def : tables) {
			if (diningTableRepository.existsByTableNumber(def.number())) {
				continue;
			}
			diningTableRepository.save(new DiningTable(def.number(), def.name(), def.capacity()));
		}
	}

	private Map<String, MenuCategory> seedCategories() {
		Map<String, String> defs = new LinkedHashMap<>();
		defs.put(CAT_STARTERS, "Салати и леки предястия");
		defs.put(CAT_MAINS, "Основни топли ястия");
		defs.put(CAT_DESSERTS, "Сладкиши");
		defs.put(CAT_DRINKS, "Безалкохолни напитки");

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
				new Ing("Маруля", IngredientUnit.GRAM, "5000", "200"),
				new Ing("Домати", IngredientUnit.GRAM, "4000", "200"),
				new Ing("Пилешко месо", IngredientUnit.GRAM, "8000", "500"),
				new Ing("Паста", IngredientUnit.GRAM, "6000", "300"),
				new Ing("Телешка кюфтета", IngredientUnit.GRAM, "5000", "300"),
				new Ing("Питка за бургер", IngredientUnit.PIECE, "200", "20"),
				new Ing("Брашно", IngredientUnit.GRAM, "10000", "500"),
				new Ing("Захар", IngredientUnit.GRAM, "5000", "200"),
				new Ing("Минерална вода (бутилка)", IngredientUnit.PIECE, "300", "20"),
				new Ing("Лимон", IngredientUnit.PIECE, "150", "10"),
				new Ing("Зеленчуков бульон", IngredientUnit.MILLILITER, "10000", "500"));

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
				new Item(CAT_STARTERS, ITEM_SALAD, "Домати, краставици, сирене и лук", "8.50"),
				new Item(CAT_STARTERS, ITEM_SOUP, "Домашна супа с пиле и зеленчуци", "6.90"),
				new Item(CAT_MAINS, ITEM_PASTA, "Пене с пилешко и сметанен сос", "14.50"),
				new Item(CAT_MAINS, ITEM_BURGER, "Телешки бургер с пържени картофи", "15.90"),
				new Item(CAT_MAINS, ITEM_CHICKEN, "Печено пиле с гарнитура", "13.50"),
				new Item(CAT_DESSERTS, ITEM_CAKE, "Домашна торта на деня", "7.50"),
				new Item(CAT_DRINKS, ITEM_WATER, "Газирана минерална вода 0.5 л", "2.50"),
				new Item(CAT_DRINKS, ITEM_LEMONADE, "Прясно изцедена лимонада", "4.20"));

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
		seedRecipeIfEmpty(items.get(ITEM_SALAD), List.of(
				entry(ingredients, "Маруля", "120"),
				entry(ingredients, "Домати", "80")));
		seedRecipeIfEmpty(items.get(ITEM_SOUP), List.of(
				entry(ingredients, "Зеленчуков бульон", "300"),
				entry(ingredients, "Домати", "50")));
		seedRecipeIfEmpty(items.get(ITEM_PASTA), List.of(
				entry(ingredients, "Паста", "180"),
				entry(ingredients, "Пилешко месо", "100")));
		seedRecipeIfEmpty(items.get(ITEM_BURGER), List.of(
				entry(ingredients, "Телешка кюфтета", "150"),
				entry(ingredients, "Питка за бургер", "1")));
		seedRecipeIfEmpty(items.get(ITEM_CHICKEN), List.of(
				entry(ingredients, "Пилешко месо", "220")));
		seedRecipeIfEmpty(items.get(ITEM_CAKE), List.of(
				entry(ingredients, "Брашно", "80"),
				entry(ingredients, "Захар", "40")));
		seedRecipeIfEmpty(items.get(ITEM_WATER), List.of(
				entry(ingredients, "Минерална вода (бутилка)", "1")));
		seedRecipeIfEmpty(items.get(ITEM_LEMONADE), List.of(
				entry(ingredients, "Лимон", "1"),
				entry(ingredients, "Захар", "20")));
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
		if (reservationRepository.findByReservationNumber(SEED_RESERVATION_NUMBER).isPresent()) {
			return;
		}
		AppUser client = appUserRepository.findByEmail(EmailNormalizer.normalize(SEED_CLIENT_EMAIL)).orElse(null);
		DiningTable table = diningTableRepository.findByTableNumber(SEED_TABLE_FOR_RESERVATION).orElse(null);
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
				SEED_RESERVATION_NUMBER,
				table,
				client,
				start,
				end,
				4,
				"Вечерна резервация за четирима",
				now));
		log.info("Demo reservation ensured");
	}
}
