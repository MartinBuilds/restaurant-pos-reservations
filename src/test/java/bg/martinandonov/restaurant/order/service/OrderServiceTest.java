package bg.martinandonov.restaurant.order.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import bg.martinandonov.restaurant.common.exception.BusinessRuleException;
import bg.martinandonov.restaurant.common.exception.InvalidRequestException;
import bg.martinandonov.restaurant.common.exception.ResourceNotFoundException;
import bg.martinandonov.restaurant.diningtable.entity.DiningTable;
import bg.martinandonov.restaurant.diningtable.entity.DiningTableStatus;
import bg.martinandonov.restaurant.diningtable.repository.DiningTableRepository;
import bg.martinandonov.restaurant.inventory.entity.Ingredient;
import bg.martinandonov.restaurant.inventory.entity.IngredientUnit;
import bg.martinandonov.restaurant.inventory.entity.RecipeIngredient;
import bg.martinandonov.restaurant.inventory.repository.IngredientRepository;
import bg.martinandonov.restaurant.inventory.repository.RecipeIngredientRepository;
import bg.martinandonov.restaurant.kitchen.websocket.dto.OrderRealtimeEventType;
import bg.martinandonov.restaurant.kitchen.websocket.event.OrderCreatedRealtimeEvent;
import bg.martinandonov.restaurant.menu.entity.MenuCategory;
import bg.martinandonov.restaurant.menu.entity.MenuItem;
import bg.martinandonov.restaurant.menu.repository.MenuItemRepository;
import bg.martinandonov.restaurant.menu.service.MenuAvailabilityService;
import bg.martinandonov.restaurant.order.dto.AddOrderItemsRequest;
import bg.martinandonov.restaurant.order.dto.CreateOrderItemRequest;
import bg.martinandonov.restaurant.order.dto.CreateOrderRequest;
import bg.martinandonov.restaurant.order.dto.OrderResponse;
import bg.martinandonov.restaurant.order.entity.OrderItem;
import bg.martinandonov.restaurant.order.entity.OrderStatus;
import bg.martinandonov.restaurant.order.entity.RestaurantOrder;
import bg.martinandonov.restaurant.order.repository.OrderItemRepository;
import bg.martinandonov.restaurant.order.repository.RestaurantOrderRepository;
import bg.martinandonov.restaurant.user.entity.AppUser;
import bg.martinandonov.restaurant.user.entity.Role;
import bg.martinandonov.restaurant.user.entity.RoleName;
import bg.martinandonov.restaurant.user.repository.AppUserRepository;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

	private static final String WAITER_EMAIL = "waiter@example.com";

	@Mock
	private RestaurantOrderRepository restaurantOrderRepository;

	@Mock
	private OrderItemRepository orderItemRepository;

	@Mock
	private DiningTableRepository diningTableRepository;

	@Mock
	private MenuItemRepository menuItemRepository;

	@Mock
	private RecipeIngredientRepository recipeIngredientRepository;

	@Mock
	private IngredientRepository ingredientRepository;

	@Mock
	private MenuAvailabilityService menuAvailabilityService;

	@Mock
	private AppUserRepository appUserRepository;

	@Mock
	private ApplicationEventPublisher applicationEventPublisher;

	@InjectMocks
	private OrderService orderService;

	private AppUser waiter;
	private MenuCategory category;

	@BeforeEach
	void setUp() {
		waiter = new AppUser(WAITER_EMAIL, "hash", "Waiter One", true);
		ReflectionTestUtils.setField(waiter, "id", 5L);
		Role waiterRole = new Role(RoleName.WAITER);
		waiter.setRoles(Set.of(waiterRole));

		category = new MenuCategory("Mains", null, true);
		ReflectionTestUtils.setField(category, "id", 1L);

		SecurityContext context = SecurityContextHolder.createEmptyContext();
		Authentication authentication = mock(Authentication.class);
		when(authentication.isAuthenticated()).thenReturn(true);
		when(authentication.getName()).thenReturn(WAITER_EMAIL);
		context.setAuthentication(authentication);
		SecurityContextHolder.setContext(context);

		when(appUserRepository.findByEmail(WAITER_EMAIL)).thenReturn(Optional.of(waiter));
	}

	@AfterEach
	void tearDown() {
		SecurityContextHolder.clearContext();
	}

	@Test
	void createOrderAcceptedOccupiesTableSnapshotsPriceAndDeductsStock() {
		DiningTable table = availableTable(1L, 3);
		MenuItem menuItem = availableMenuItem(10L, "Grilled Salmon", "12.50");
		Ingredient ingredient = activeIngredient(100L, "10.000");
		RecipeIngredient recipe = recipeLine(menuItem, ingredient, "1.500");

		stubCreateOrderTable(table);
		stubMenuItemLoad(menuItem, recipe);
		stubStockDeduction(ingredient);

		List<OrderItem> persistedItems = new ArrayList<>();
		when(restaurantOrderRepository.save(any(RestaurantOrder.class))).thenAnswer(invocation -> {
			RestaurantOrder order = invocation.getArgument(0);
			ReflectionTestUtils.setField(order, "id", 50L);
			return order;
		});
		when(orderItemRepository.save(any(OrderItem.class))).thenAnswer(invocation -> {
			OrderItem item = invocation.getArgument(0);
			ReflectionTestUtils.setField(item, "id", (long) (persistedItems.size() + 1));
			persistedItems.add(item);
			return item;
		});
		when(orderItemRepository.findByOrderIdOrderByIdAsc(50L)).thenAnswer(invocation -> List.copyOf(persistedItems));

		CreateOrderRequest request = createOrderRequest(1L, orderLine(10L, 2));

		OrderResponse response = orderService.createOrder(request);

		ArgumentCaptor<RestaurantOrder> orderCaptor = ArgumentCaptor.forClass(RestaurantOrder.class);
		verify(restaurantOrderRepository).save(orderCaptor.capture());
		RestaurantOrder savedOrder = orderCaptor.getValue();
		assertThat(savedOrder.getOrderNumber()).isNotBlank().hasSize(36);
		assertThat(savedOrder.getStatus()).isEqualTo(OrderStatus.ACCEPTED);
		assertThat(savedOrder.isClosed()).isFalse();
		assertThat(savedOrder.getTotalAmount()).isEqualByComparingTo("25.00");

		assertThat(table.getStatus()).isEqualTo(DiningTableStatus.OCCUPIED);
		assertThat(ingredient.getStockQuantity()).isEqualByComparingTo("7.000");

		assertThat(response.getStatus()).isEqualTo("ACCEPTED");
		assertThat(response.isClosed()).isFalse();
		assertThat(response.getTotalAmount()).isEqualByComparingTo("25.00");
		assertThat(response.getItems()).hasSize(1);
		assertThat(response.getItems().get(0).getMenuItemName()).isEqualTo("Grilled Salmon");
		assertThat(response.getItems().get(0).getUnitPrice()).isEqualByComparingTo("12.50");
		assertThat(response.getItems().get(0).getQuantity()).isEqualTo(2);
		assertThat(response.getItems().get(0).getLineTotal()).isEqualByComparingTo("25.00");

		verify(menuAvailabilityService).recalculateForIngredient(100L);

		ArgumentCaptor<OrderCreatedRealtimeEvent> eventCaptor =
				ArgumentCaptor.forClass(OrderCreatedRealtimeEvent.class);
		verify(applicationEventPublisher).publishEvent(eventCaptor.capture());
		assertThat(eventCaptor.getValue().getMessage().getEventType())
				.isEqualTo(OrderRealtimeEventType.ORDER_CREATED);
		assertThat(eventCaptor.getValue().getMessage().getPreviousStatus()).isNull();
		assertThat(eventCaptor.getValue().getMessage().getCurrentStatus()).isEqualTo(OrderStatus.ACCEPTED);
		assertThat(eventCaptor.getValue().getMessage().getEventId()).isNotNull();
		assertThat(eventCaptor.getValue().getMessage().getOrder().getId()).isEqualTo(50L);
		assertThat(eventCaptor.getValue().getMessage().getOrder().getItems()).hasSize(1);
		assertThat(eventCaptor.getValue().getMessage().getOrder().getItems().get(0).getQuantity())
				.isEqualTo(2);
		assertThat(eventCaptor.getValue().getMessage().getOrder().getItems().get(0).getMenuItemName())
				.isEqualTo("Grilled Salmon");
	}

	@Test
	void createOrderAggregatesSharedIngredientAcrossDishes() {
		DiningTable table = availableTable(1L, 1);
		MenuItem dishA = availableMenuItem(10L, "Dish A", "5.00");
		MenuItem dishB = availableMenuItem(11L, "Dish B", "7.00");
		Ingredient shared = activeIngredient(200L, "20.000");
		RecipeIngredient recipeA = recipeLine(dishA, shared, "1.000");
		RecipeIngredient recipeB = recipeLine(dishB, shared, "0.500");

		stubCreateOrderTable(table);
		when(menuItemRepository.findAllByIdInWithCategoryOrderByIdAsc(Set.of(10L, 11L)))
				.thenReturn(List.of(dishA, dishB));
		when(recipeIngredientRepository.findByMenuItemIdOrderByIngredientNameAsc(10L)).thenReturn(List.of(recipeA));
		when(recipeIngredientRepository.findByMenuItemIdOrderByIngredientNameAsc(11L)).thenReturn(List.of(recipeB));
		when(ingredientRepository.findAllByIdInOrderByIdAscForUpdate(Set.of(200L))).thenReturn(List.of(shared));

		List<OrderItem> persistedItems = new ArrayList<>();
		when(restaurantOrderRepository.save(any(RestaurantOrder.class))).thenAnswer(invocation -> {
			RestaurantOrder order = invocation.getArgument(0);
			ReflectionTestUtils.setField(order, "id", 60L);
			return order;
		});
		when(orderItemRepository.save(any(OrderItem.class))).thenAnswer(invocation -> {
			OrderItem item = invocation.getArgument(0);
			ReflectionTestUtils.setField(item, "id", (long) (persistedItems.size() + 1));
			persistedItems.add(item);
			return item;
		});
		when(orderItemRepository.findByOrderIdOrderByIdAsc(60L)).thenReturn(List.copyOf(persistedItems));

		CreateOrderRequest request = createOrderRequest(1L, orderLine(10L, 2), orderLine(11L, 4));

		OrderResponse response = orderService.createOrder(request);

		assertThat(shared.getStockQuantity()).isEqualByComparingTo("16.000");
		assertThat(response.getTotalAmount()).isEqualByComparingTo("38.00");
		verify(menuAvailabilityService).recalculateForIngredient(200L);
	}

	@Test
	void createOrderRejectsEmptyItems() {
		CreateOrderRequest request = createOrderRequest(1L);

		assertThatThrownBy(() -> orderService.createOrder(request))
				.isInstanceOf(InvalidRequestException.class)
				.hasMessageContaining("at least one item");
		verify(restaurantOrderRepository, never()).save(any());
	}

	@Test
	void createOrderRejectsDuplicateMenuItemIds() {
		CreateOrderRequest request = createOrderRequest(1L, orderLine(10L, 1), orderLine(10L, 2));

		assertThatThrownBy(() -> orderService.createOrder(request))
				.isInstanceOf(InvalidRequestException.class)
				.hasMessageContaining("Duplicate");
		verify(restaurantOrderRepository, never()).save(any());
	}

	@Test
	void createOrderRejectsZeroQuantity() {
		CreateOrderRequest request = createOrderRequest(1L, orderLine(10L, 0));

		assertThatThrownBy(() -> orderService.createOrder(request))
				.isInstanceOf(InvalidRequestException.class)
				.hasMessageContaining("between 1 and 100");
	}

	@Test
	void createOrderRejectsQuantityOver100() {
		CreateOrderRequest request = createOrderRequest(1L, orderLine(10L, 101));

		assertThatThrownBy(() -> orderService.createOrder(request))
				.isInstanceOf(InvalidRequestException.class)
				.hasMessageContaining("between 1 and 100");
	}

	@Test
	void createOrderMissingTableReturns404() {
		when(diningTableRepository.findByIdForUpdate(99L)).thenReturn(Optional.empty());

		CreateOrderRequest request = createOrderRequest(99L, orderLine(10L, 1));

		assertThatThrownBy(() -> orderService.createOrder(request))
				.isInstanceOf(ResourceNotFoundException.class)
				.hasMessageContaining("99");
		verify(restaurantOrderRepository, never()).save(any());
	}

	@Test
	void createOrderRejectsInactiveTable() {
		DiningTable table = availableTable(1L, 1);
		table.setActive(false);
		table.setStatus(DiningTableStatus.OUT_OF_SERVICE);
		when(diningTableRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(table));

		assertThatThrownBy(() -> orderService.createOrder(createOrderRequest(1L, orderLine(10L, 1))))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("inactive");
	}

	@Test
	void createOrderRejectsNonAvailableTable() {
		DiningTable table = availableTable(1L, 1);
		table.setStatus(DiningTableStatus.OCCUPIED);
		when(diningTableRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(table));

		assertThatThrownBy(() -> orderService.createOrder(createOrderRequest(1L, orderLine(10L, 1))))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("not AVAILABLE");
	}

	@Test
	void createOrderRejectsOpenOrderOnTable() {
		DiningTable table = availableTable(1L, 1);
		when(diningTableRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(table));
		when(restaurantOrderRepository.existsByDiningTableIdAndClosedFalse(1L)).thenReturn(true);

		assertThatThrownBy(() -> orderService.createOrder(createOrderRequest(1L, orderLine(10L, 1))))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("open order");
	}

	@Test
	void createOrderMissingMenuItemReturns404() {
		stubCreateOrderTable(availableTable(1L, 1));
		when(menuItemRepository.findAllByIdInWithCategoryOrderByIdAsc(Set.of(10L))).thenReturn(List.of());

		assertThatThrownBy(() -> orderService.createOrder(createOrderRequest(1L, orderLine(10L, 1))))
				.isInstanceOf(ResourceNotFoundException.class)
				.hasMessageContaining("Menu item not found: 10");
	}

	@Test
	void createOrderRejectsInactiveMenuItem() {
		MenuItem menuItem = availableMenuItem(10L, "Soup", "4.00");
		menuItem.setActive(false);
		stubCreateOrderTable(availableTable(1L, 1));
		when(menuItemRepository.findAllByIdInWithCategoryOrderByIdAsc(Set.of(10L))).thenReturn(List.of(menuItem));

		assertThatThrownBy(() -> orderService.createOrder(createOrderRequest(1L, orderLine(10L, 1))))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("inactive: 10");
	}

	@Test
	void createOrderRejectsInactiveCategory() {
		MenuItem menuItem = availableMenuItem(10L, "Soup", "4.00");
		category.setActive(false);
		stubCreateOrderTable(availableTable(1L, 1));
		when(menuItemRepository.findAllByIdInWithCategoryOrderByIdAsc(Set.of(10L))).thenReturn(List.of(menuItem));

		assertThatThrownBy(() -> orderService.createOrder(createOrderRequest(1L, orderLine(10L, 1))))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("category is inactive");
	}

	@Test
	void createOrderRejectsUnavailableMenuItem() {
		MenuItem menuItem = availableMenuItem(10L, "Soup", "4.00");
		menuItem.setAvailable(false);
		stubCreateOrderTable(availableTable(1L, 1));
		when(menuItemRepository.findAllByIdInWithCategoryOrderByIdAsc(Set.of(10L))).thenReturn(List.of(menuItem));

		assertThatThrownBy(() -> orderService.createOrder(createOrderRequest(1L, orderLine(10L, 1))))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("unavailable: 10");
	}

	@Test
	void createOrderRejectsMenuItemWithoutRecipe() {
		MenuItem menuItem = availableMenuItem(10L, "Soup", "4.00");
		stubCreateOrderTable(availableTable(1L, 1));
		when(menuItemRepository.findAllByIdInWithCategoryOrderByIdAsc(Set.of(10L))).thenReturn(List.of(menuItem));
		when(recipeIngredientRepository.findByMenuItemIdOrderByIngredientNameAsc(10L)).thenReturn(List.of());

		assertThatThrownBy(() -> orderService.createOrder(createOrderRequest(1L, orderLine(10L, 1))))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("no recipe");
	}

	@Test
	void createOrderRejectsInactiveIngredientInRecipe() {
		MenuItem menuItem = availableMenuItem(10L, "Soup", "4.00");
		Ingredient ingredient = activeIngredient(100L, "5.000");
		ingredient.setActive(false);
		RecipeIngredient recipe = recipeLine(menuItem, ingredient, "1.000");
		stubCreateOrderTable(availableTable(1L, 1));
		stubMenuItemLoad(menuItem, recipe);

		assertThatThrownBy(() -> orderService.createOrder(createOrderRequest(1L, orderLine(10L, 1))))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("Ingredient is inactive: 100");
	}

	@Test
	void createOrderRejectsInsufficientStockWithoutPersistingOrderOrOccupyingTable() {
		DiningTable table = availableTable(1L, 2);
		MenuItem menuItem = availableMenuItem(10L, "Steak", "20.00");
		Ingredient ingredient = activeIngredient(100L, "1.000");
		RecipeIngredient recipe = recipeLine(menuItem, ingredient, "2.000");

		stubCreateOrderTable(table);
		stubMenuItemLoad(menuItem, recipe);
		when(ingredientRepository.findAllByIdInOrderByIdAscForUpdate(Set.of(100L))).thenReturn(List.of(ingredient));

		assertThatThrownBy(() -> orderService.createOrder(createOrderRequest(1L, orderLine(10L, 1))))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("Insufficient stock");

		verify(restaurantOrderRepository, never()).save(any());
		assertThat(table.getStatus()).isEqualTo(DiningTableStatus.AVAILABLE);
		verify(orderItemRepository, never()).save(any());
		verify(applicationEventPublisher, never()).publishEvent(any());
	}

	@Test
	void addItemsCreatesNewLineAndRecalculatesTotal() {
		DiningTable table = occupiedTable(1L, 4);
		RestaurantOrder order = openAcceptedOrder(70L, table, waiter);
		MenuItem menuItem = availableMenuItem(20L, "Salad", "8.25");
		Ingredient ingredient = activeIngredient(300L, "15.000");
		RecipeIngredient recipe = recipeLine(menuItem, ingredient, "0.250");

		when(restaurantOrderRepository.findByIdForUpdate(70L)).thenReturn(Optional.of(order));
		when(diningTableRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(table));
		stubMenuItemLoad(menuItem, recipe);
		stubStockDeduction(ingredient);
		when(orderItemRepository.findByOrderIdAndMenuItemId(70L, 20L)).thenReturn(Optional.empty());

		List<OrderItem> lines = new ArrayList<>();
		when(orderItemRepository.save(any(OrderItem.class))).thenAnswer(invocation -> {
			OrderItem item = invocation.getArgument(0);
			ReflectionTestUtils.setField(item, "id", 901L);
			lines.add(item);
			return item;
		});
		when(orderItemRepository.findByOrderIdOrderByIdAsc(70L)).thenReturn(lines);

		AddOrderItemsRequest request = addItemsRequest(orderLine(20L, 3));
		OrderResponse response = orderService.addItemsToOrder(70L, request);

		assertThat(lines).hasSize(1);
		assertThat(lines.get(0).getUnitPrice()).isEqualByComparingTo("8.25");
		assertThat(lines.get(0).getQuantity()).isEqualTo(3);
		assertThat(lines.get(0).getLineTotal()).isEqualByComparingTo("24.75");
		assertThat(order.getTotalAmount()).isEqualByComparingTo("24.75");
		assertThat(response.getTotalAmount()).isEqualByComparingTo("24.75");
	}

	@Test
	void addItemsIncreasesExistingQuantityKeepingSnapshotUnitPrice() {
		DiningTable table = occupiedTable(1L, 4);
		RestaurantOrder order = openAcceptedOrder(80L, table, waiter);
		MenuItem menuItem = availableMenuItem(20L, "Salad", "15.00");
		menuItem.setPrice(new BigDecimal("99.99"));
		Ingredient ingredient = activeIngredient(300L, "15.000");
		RecipeIngredient recipe = recipeLine(menuItem, ingredient, "0.250");

		OrderItem existing = new OrderItem(
				order,
				menuItem,
				"Salad",
				new BigDecimal("8.25"),
				2,
				new BigDecimal("16.50"));
		ReflectionTestUtils.setField(existing, "id", 800L);

		when(restaurantOrderRepository.findByIdForUpdate(80L)).thenReturn(Optional.of(order));
		when(diningTableRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(table));
		stubMenuItemLoad(menuItem, recipe);
		stubStockDeduction(ingredient);
		when(orderItemRepository.findByOrderIdAndMenuItemId(80L, 20L)).thenReturn(Optional.of(existing));
		when(orderItemRepository.findByOrderIdOrderByIdAsc(80L)).thenReturn(List.of(existing));

		orderService.addItemsToOrder(80L, addItemsRequest(orderLine(20L, 1)));

		assertThat(existing.getQuantity()).isEqualTo(3);
		assertThat(existing.getUnitPrice()).isEqualByComparingTo("8.25");
		assertThat(existing.getLineTotal()).isEqualByComparingTo("24.75");
		verify(orderItemRepository, never()).save(any());
	}

	@Test
	void addItemsRejectsQuantityOver100() {
		DiningTable table = occupiedTable(1L, 4);
		RestaurantOrder order = openAcceptedOrder(81L, table, waiter);
		MenuItem menuItem = availableMenuItem(20L, "Salad", "8.25");
		Ingredient ingredient = activeIngredient(300L, "100.000");
		RecipeIngredient recipe = recipeLine(menuItem, ingredient, "0.100");

		OrderItem existing = new OrderItem(
				order,
				menuItem,
				"Salad",
				new BigDecimal("8.25"),
				95,
				new BigDecimal("783.75"));
		ReflectionTestUtils.setField(existing, "id", 801L);

		when(restaurantOrderRepository.findByIdForUpdate(81L)).thenReturn(Optional.of(order));
		when(diningTableRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(table));
		stubMenuItemLoad(menuItem, recipe);
		when(ingredientRepository.findAllByIdInOrderByIdAscForUpdate(Set.of(300L))).thenReturn(List.of(ingredient));
		when(orderItemRepository.findByOrderIdAndMenuItemId(81L, 20L)).thenReturn(Optional.of(existing));

		assertThatThrownBy(() -> orderService.addItemsToOrder(81L, addItemsRequest(orderLine(20L, 10))))
				.isInstanceOf(InvalidRequestException.class)
				.hasMessageContaining("at most 100");
	}

	@Test
	void addItemsRejectsClosedOrder() {
		DiningTable table = occupiedTable(1L, 4);
		RestaurantOrder order = openAcceptedOrder(82L, table, waiter);
		order.setClosed(true);
		when(restaurantOrderRepository.findByIdForUpdate(82L)).thenReturn(Optional.of(order));

		assertThatThrownBy(() -> orderService.addItemsToOrder(82L, addItemsRequest(orderLine(20L, 1))))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("closed order");
		verify(diningTableRepository, never()).findByIdForUpdate(anyLong());
	}

	@Test
	void addItemsRejectsNonAcceptedOrder() {
		DiningTable table = occupiedTable(1L, 4);
		RestaurantOrder order = openAcceptedOrder(83L, table, waiter);
		order.setStatus(OrderStatus.COOKING);
		when(restaurantOrderRepository.findByIdForUpdate(83L)).thenReturn(Optional.of(order));

		assertThatThrownBy(() -> orderService.addItemsToOrder(83L, addItemsRequest(orderLine(20L, 1))))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("ACCEPTED");
	}

	private void stubCreateOrderTable(DiningTable table) {
		when(diningTableRepository.findByIdForUpdate(table.getId())).thenReturn(Optional.of(table));
		when(restaurantOrderRepository.existsByDiningTableIdAndClosedFalse(table.getId())).thenReturn(false);
	}

	private void stubMenuItemLoad(MenuItem menuItem, RecipeIngredient recipe) {
		when(menuItemRepository.findAllByIdInWithCategoryOrderByIdAsc(Set.of(menuItem.getId())))
				.thenReturn(List.of(menuItem));
		when(recipeIngredientRepository.findByMenuItemIdOrderByIngredientNameAsc(menuItem.getId()))
				.thenReturn(List.of(recipe));
	}

	private void stubStockDeduction(Ingredient ingredient) {
		when(ingredientRepository.findAllByIdInOrderByIdAscForUpdate(Set.of(ingredient.getId())))
				.thenReturn(List.of(ingredient));
	}

	private DiningTable availableTable(Long id, int tableNumber) {
		DiningTable table = new DiningTable(tableNumber, null, 4);
		ReflectionTestUtils.setField(table, "id", id);
		table.setStatus(DiningTableStatus.AVAILABLE);
		table.setActive(true);
		return table;
	}

	private DiningTable occupiedTable(Long id, int tableNumber) {
		DiningTable table = availableTable(id, tableNumber);
		table.setStatus(DiningTableStatus.OCCUPIED);
		return table;
	}

	private MenuItem availableMenuItem(Long id, String name, String price) {
		MenuItem item = new MenuItem(name, null, new BigDecimal(price), true, true, category);
		ReflectionTestUtils.setField(item, "id", id);
		item.setAvailable(true);
		return item;
	}

	private Ingredient activeIngredient(Long id, String stock) {
		Ingredient ingredient = new Ingredient(
				"Ingredient-" + id,
				IngredientUnit.GRAM,
				new BigDecimal(stock),
				new BigDecimal("1.000"),
				true);
		ReflectionTestUtils.setField(ingredient, "id", id);
		return ingredient;
	}

	private RecipeIngredient recipeLine(MenuItem menuItem, Ingredient ingredient, String qty) {
		return new RecipeIngredient(menuItem, ingredient, new BigDecimal(qty));
	}

	private RestaurantOrder openAcceptedOrder(Long id, DiningTable table, AppUser orderWaiter) {
		RestaurantOrder order = new RestaurantOrder("order-uuid", table, orderWaiter, java.time.LocalDateTime.now());
		ReflectionTestUtils.setField(order, "id", id);
		order.setStatus(OrderStatus.ACCEPTED);
		order.setClosed(false);
		return order;
	}

	private CreateOrderRequest createOrderRequest(Long tableId, CreateOrderItemRequest... lines) {
		CreateOrderRequest request = new CreateOrderRequest();
		request.setDiningTableId(tableId);
		request.setItems(new ArrayList<>(List.of(lines)));
		return request;
	}

	private AddOrderItemsRequest addItemsRequest(CreateOrderItemRequest... lines) {
		AddOrderItemsRequest request = new AddOrderItemsRequest();
		request.setItems(new ArrayList<>(List.of(lines)));
		return request;
	}

	private CreateOrderItemRequest orderLine(Long menuItemId, int quantity) {
		CreateOrderItemRequest line = new CreateOrderItemRequest();
		line.setMenuItemId(menuItemId);
		line.setQuantity(quantity);
		return line;
	}
}