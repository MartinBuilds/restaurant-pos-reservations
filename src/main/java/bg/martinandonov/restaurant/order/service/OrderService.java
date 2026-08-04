package bg.martinandonov.restaurant.order.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import bg.martinandonov.restaurant.common.exception.BusinessRuleException;
import bg.martinandonov.restaurant.common.exception.InvalidRequestException;
import bg.martinandonov.restaurant.common.exception.ResourceNotFoundException;
import bg.martinandonov.restaurant.diningtable.entity.DiningTable;
import bg.martinandonov.restaurant.diningtable.entity.DiningTableStatus;
import bg.martinandonov.restaurant.diningtable.repository.DiningTableRepository;
import bg.martinandonov.restaurant.inventory.entity.Ingredient;
import bg.martinandonov.restaurant.inventory.entity.RecipeIngredient;
import bg.martinandonov.restaurant.inventory.repository.IngredientRepository;
import bg.martinandonov.restaurant.inventory.repository.RecipeIngredientRepository;
import bg.martinandonov.restaurant.kitchen.websocket.dto.OrderRealtimeMessage;
import bg.martinandonov.restaurant.kitchen.websocket.event.OrderCreatedRealtimeEvent;
import bg.martinandonov.restaurant.menu.entity.MenuItem;
import bg.martinandonov.restaurant.menu.repository.MenuItemRepository;
import bg.martinandonov.restaurant.menu.service.MenuAvailabilityService;
import bg.martinandonov.restaurant.order.dto.AddOrderItemsRequest;
import bg.martinandonov.restaurant.order.dto.CreateOrderItemRequest;
import bg.martinandonov.restaurant.order.dto.CreateOrderRequest;
import bg.martinandonov.restaurant.order.dto.KitchenOrderItemResponse;
import bg.martinandonov.restaurant.order.dto.KitchenOrderResponse;
import bg.martinandonov.restaurant.order.dto.OrderItemResponse;
import bg.martinandonov.restaurant.order.dto.OrderResponse;
import bg.martinandonov.restaurant.order.entity.OrderItem;
import bg.martinandonov.restaurant.order.entity.OrderStatus;
import bg.martinandonov.restaurant.order.entity.RestaurantOrder;
import bg.martinandonov.restaurant.order.repository.OrderItemRepository;
import bg.martinandonov.restaurant.order.repository.RestaurantOrderRepository;
import bg.martinandonov.restaurant.user.entity.AppUser;
import bg.martinandonov.restaurant.user.entity.RoleName;
import bg.martinandonov.restaurant.user.repository.AppUserRepository;

@Service
@Transactional
public class OrderService {

	private static final int MIN_QUANTITY = 1;
	private static final int MAX_QUANTITY = 100;
	private static final int MONEY_SCALE = 2;

	private final RestaurantOrderRepository restaurantOrderRepository;
	private final OrderItemRepository orderItemRepository;
	private final DiningTableRepository diningTableRepository;
	private final MenuItemRepository menuItemRepository;
	private final RecipeIngredientRepository recipeIngredientRepository;
	private final IngredientRepository ingredientRepository;
	private final MenuAvailabilityService menuAvailabilityService;
	private final AppUserRepository appUserRepository;
	private final ApplicationEventPublisher applicationEventPublisher;

	public OrderService(
			RestaurantOrderRepository restaurantOrderRepository,
			OrderItemRepository orderItemRepository,
			DiningTableRepository diningTableRepository,
			MenuItemRepository menuItemRepository,
			RecipeIngredientRepository recipeIngredientRepository,
			IngredientRepository ingredientRepository,
			MenuAvailabilityService menuAvailabilityService,
			AppUserRepository appUserRepository,
			ApplicationEventPublisher applicationEventPublisher) {
		this.restaurantOrderRepository = restaurantOrderRepository;
		this.orderItemRepository = orderItemRepository;
		this.diningTableRepository = diningTableRepository;
		this.menuItemRepository = menuItemRepository;
		this.recipeIngredientRepository = recipeIngredientRepository;
		this.ingredientRepository = ingredientRepository;
		this.menuAvailabilityService = menuAvailabilityService;
		this.appUserRepository = appUserRepository;
		this.applicationEventPublisher = applicationEventPublisher;
	}

	public OrderResponse createOrder(CreateOrderRequest request) {
		Objects.requireNonNull(request, "request must not be null");
		AppUser waiter = requireAuthenticatedWaiterOrAdmin();
		List<CreateOrderItemRequest> items = requireItems(request.getItems());
		Map<Long, Integer> requestedQuantities = validateAndIndexItems(items);

		DiningTable table = diningTableRepository.findByIdForUpdate(request.getDiningTableId())
				.orElseThrow(() -> new ResourceNotFoundException(
						"Dining table not found: " + request.getDiningTableId()));
		assertTableAvailableForNewOrder(table);

		Map<Long, MenuItem> menuItems = loadAndValidateMenuItems(requestedQuantities.keySet());
		Map<Long, BigDecimal> requiredStock = aggregateRequiredStock(requestedQuantities, menuItems);
		deductStock(requiredStock);

		LocalDateTime now = LocalDateTime.now();
		RestaurantOrder order = new RestaurantOrder(generateOrderNumber(), table, waiter, now);
		RestaurantOrder savedOrder = restaurantOrderRepository.save(order);

		BigDecimal total = BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
		for (Map.Entry<Long, Integer> entry : requestedQuantities.entrySet()) {
			MenuItem menuItem = menuItems.get(entry.getKey());
			Integer quantity = entry.getValue();
			BigDecimal unitPrice = money(menuItem.getPrice());
			BigDecimal lineTotal = unitPrice.multiply(BigDecimal.valueOf(quantity))
					.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
			OrderItem orderItem = new OrderItem(
					savedOrder,
					menuItem,
					menuItem.getName(),
					unitPrice,
					quantity,
					lineTotal);
			orderItemRepository.save(orderItem);
			total = total.add(lineTotal);
		}

		savedOrder.setTotalAmount(total);
		savedOrder.setUpdatedAt(now);
		table.setStatus(DiningTableStatus.OCCUPIED);
		recalculateAvailabilityForIngredients(requiredStock.keySet());

		KitchenOrderResponse kitchenSnapshot = toKitchenResponse(savedOrder);
		applicationEventPublisher.publishEvent(
				new OrderCreatedRealtimeEvent(OrderRealtimeMessage.orderCreated(kitchenSnapshot)));

		return toResponse(savedOrder);
	}

	public OrderResponse addItemsToOrder(Long orderId, AddOrderItemsRequest request) {
		Objects.requireNonNull(request, "request must not be null");
		requireAuthenticatedWaiterOrAdmin();
		List<CreateOrderItemRequest> items = requireItems(request.getItems());
		Map<Long, Integer> requestedQuantities = validateAndIndexItems(items);

		RestaurantOrder order = restaurantOrderRepository.findByIdForUpdate(orderId)
				.orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));
		assertOrderAcceptsItems(order);

		DiningTable table = diningTableRepository.findByIdForUpdate(order.getDiningTable().getId())
				.orElseThrow(() -> new ResourceNotFoundException(
						"Dining table not found: " + order.getDiningTable().getId()));
		assertTableOccupiedForAddItems(table);

		Map<Long, MenuItem> menuItems = loadAndValidateMenuItems(requestedQuantities.keySet());
		Map<Long, BigDecimal> requiredStock = aggregateRequiredStock(requestedQuantities, menuItems);
		deductStock(requiredStock);

		LocalDateTime now = LocalDateTime.now();
		for (Map.Entry<Long, Integer> entry : requestedQuantities.entrySet()) {
			Long menuItemId = entry.getKey();
			Integer addedQuantity = entry.getValue();
			MenuItem menuItem = menuItems.get(menuItemId);
			OrderItem existing = orderItemRepository.findByOrderIdAndMenuItemId(order.getId(), menuItemId)
					.orElse(null);
			if (existing != null) {
				int newQuantity = existing.getQuantity() + addedQuantity;
				if (newQuantity > MAX_QUANTITY) {
					throw new InvalidRequestException("Order item quantity must be at most " + MAX_QUANTITY);
				}
				BigDecimal lineTotal = existing.getUnitPrice()
						.multiply(BigDecimal.valueOf(newQuantity))
						.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
				existing.setQuantity(newQuantity);
				existing.setLineTotal(lineTotal);
			}
			else {
				BigDecimal unitPrice = money(menuItem.getPrice());
				BigDecimal lineTotal = unitPrice.multiply(BigDecimal.valueOf(addedQuantity))
						.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
				orderItemRepository.save(new OrderItem(
						order,
						menuItem,
						menuItem.getName(),
						unitPrice,
						addedQuantity,
						lineTotal));
			}
		}

		BigDecimal total = orderItemRepository.findByOrderIdOrderByIdAsc(order.getId()).stream()
				.map(OrderItem::getLineTotal)
				.reduce(BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP), BigDecimal::add);
		order.setTotalAmount(total);
		order.setUpdatedAt(now);
		recalculateAvailabilityForIngredients(requiredStock.keySet());

		return toResponse(order);
	}

	@Transactional(readOnly = true)
	public OrderResponse getOrderById(Long id) {
		RestaurantOrder order = restaurantOrderRepository.findByIdWithDetails(id)
				.orElseThrow(() -> new ResourceNotFoundException("Order not found: " + id));
		return toResponse(order);
	}

	@Transactional(readOnly = true)
	public List<OrderResponse> getOpenOrders() {
		return restaurantOrderRepository.findOpenOrdersWithDetails().stream()
				.map(this::toResponse)
				.toList();
	}

	@Transactional(readOnly = true)
	public List<OrderResponse> getOpenOrdersByTable(Long tableId) {
		if (tableId == null) {
			throw new InvalidRequestException("tableId must be provided");
		}
		if (!diningTableRepository.existsById(tableId)) {
			throw new ResourceNotFoundException("Dining table not found: " + tableId);
		}
		return restaurantOrderRepository.findOpenOrdersByTableWithDetails(tableId).stream()
				.map(this::toResponse)
				.toList();
	}

	private AppUser requireAuthenticatedWaiterOrAdmin() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !authentication.isAuthenticated()
				|| authentication.getName() == null
				|| "anonymousUser".equals(authentication.getName())) {
			throw new AccessDeniedException("Authentication required");
		}
		AppUser user = appUserRepository.findByEmail(authentication.getName())
				.orElseThrow(() -> new AccessDeniedException("Authenticated user was not found"));
		if (!user.isEnabled()) {
			throw new AccessDeniedException("Authenticated user is disabled");
		}
		boolean allowed = user.getRoles().stream()
				.anyMatch(role -> role.getName() == RoleName.WAITER || role.getName() == RoleName.ADMIN);
		if (!allowed) {
			throw new AccessDeniedException("Only WAITER or ADMIN can manage orders");
		}
		return user;
	}

	private List<CreateOrderItemRequest> requireItems(List<CreateOrderItemRequest> items) {
		if (items == null || items.isEmpty()) {
			throw new InvalidRequestException("Order must contain at least one item");
		}
		return items;
	}

	private Map<Long, Integer> validateAndIndexItems(List<CreateOrderItemRequest> items) {
		Map<Long, Integer> quantities = new HashMap<>();
		Set<Long> seen = new HashSet<>();
		for (CreateOrderItemRequest item : items) {
			if (item == null || item.getMenuItemId() == null) {
				throw new InvalidRequestException("menuItemId must be provided");
			}
			if (!seen.add(item.getMenuItemId())) {
				throw new InvalidRequestException("Duplicate menu item ids are not allowed in a request");
			}
			Integer quantity = requireQuantity(item.getQuantity());
			quantities.put(item.getMenuItemId(), quantity);
		}
		return quantities;
	}

	private Integer requireQuantity(Integer quantity) {
		if (quantity == null) {
			throw new InvalidRequestException("quantity must be provided");
		}
		if (quantity < MIN_QUANTITY || quantity > MAX_QUANTITY) {
			throw new InvalidRequestException(
					"quantity must be between " + MIN_QUANTITY + " and " + MAX_QUANTITY);
		}
		return quantity;
	}

	private void assertTableAvailableForNewOrder(DiningTable table) {
		if (!table.isActive()) {
			throw new BusinessRuleException("Dining table is inactive");
		}
		if (table.getStatus() != DiningTableStatus.AVAILABLE) {
			throw new BusinessRuleException("Dining table is not AVAILABLE");
		}
		if (restaurantOrderRepository.existsByDiningTableIdAndClosedFalse(table.getId())) {
			throw new BusinessRuleException("An open order already exists for this dining table");
		}
	}

	private void assertTableOccupiedForAddItems(DiningTable table) {
		if (!table.isActive()) {
			throw new BusinessRuleException("Dining table is inactive");
		}
		if (table.getStatus() != DiningTableStatus.OCCUPIED) {
			throw new BusinessRuleException("Dining table must be OCCUPIED to add order items");
		}
	}

	private void assertOrderAcceptsItems(RestaurantOrder order) {
		if (order.isClosed()) {
			throw new BusinessRuleException("Cannot add items to a closed order");
		}
		if (order.getStatus() != OrderStatus.ACCEPTED) {
			throw new BusinessRuleException("Items can only be added to orders in ACCEPTED status");
		}
	}

	private Map<Long, MenuItem> loadAndValidateMenuItems(Set<Long> menuItemIds) {
		List<MenuItem> loaded = menuItemRepository.findAllByIdInWithCategoryOrderByIdAsc(menuItemIds);
		Map<Long, MenuItem> byId = new HashMap<>();
		for (MenuItem item : loaded) {
			byId.put(item.getId(), item);
		}
		for (Long menuItemId : menuItemIds) {
			MenuItem item = byId.get(menuItemId);
			if (item == null) {
				throw new ResourceNotFoundException("Menu item not found: " + menuItemId);
			}
			if (!item.isActive()) {
				throw new BusinessRuleException("Menu item is inactive: " + menuItemId);
			}
			if (!item.getCategory().isActive()) {
				throw new BusinessRuleException("Menu category is inactive for item: " + menuItemId);
			}
			if (!item.isManualAvailable() || !item.isAvailable()) {
				throw new BusinessRuleException("Menu item is unavailable: " + menuItemId);
			}
			List<RecipeIngredient> recipe =
					recipeIngredientRepository.findByMenuItemIdOrderByIngredientNameAsc(menuItemId);
			if (recipe.isEmpty()) {
				throw new BusinessRuleException("Menu item has no recipe: " + menuItemId);
			}
		}
		return byId;
	}

	private Map<Long, BigDecimal> aggregateRequiredStock(
			Map<Long, Integer> requestedQuantities,
			Map<Long, MenuItem> menuItems) {
		Map<Long, BigDecimal> required = new TreeMap<>();
		for (Map.Entry<Long, Integer> entry : requestedQuantities.entrySet()) {
			Long menuItemId = entry.getKey();
			Integer quantity = entry.getValue();
			List<RecipeIngredient> recipe =
					recipeIngredientRepository.findByMenuItemIdOrderByIngredientNameAsc(menuItemId);
			for (RecipeIngredient component : recipe) {
				Ingredient ingredient = component.getIngredient();
				if (ingredient == null || ingredient.getId() == null) {
					throw new ResourceNotFoundException("Ingredient not found for menu item: " + menuItemId);
				}
				if (!ingredient.isActive()) {
					throw new BusinessRuleException("Ingredient is inactive: " + ingredient.getId());
				}
				BigDecimal needed = component.getQuantityRequired()
						.multiply(BigDecimal.valueOf(quantity));
				required.merge(ingredient.getId(), needed, BigDecimal::add);
			}
		}
		return required;
	}

	private void deductStock(Map<Long, BigDecimal> requiredStock) {
		if (requiredStock.isEmpty()) {
			return;
		}
		List<Ingredient> ingredients =
				ingredientRepository.findAllByIdInOrderByIdAscForUpdate(requiredStock.keySet());
		if (ingredients.size() != requiredStock.size()) {
			throw new ResourceNotFoundException("One or more ingredients were not found");
		}
		for (Ingredient ingredient : ingredients) {
			BigDecimal needed = requiredStock.get(ingredient.getId());
			if (!ingredient.isActive()) {
				throw new BusinessRuleException("Ingredient is inactive: " + ingredient.getId());
			}
			if (ingredient.getStockQuantity().compareTo(needed) < 0) {
				throw new BusinessRuleException("Insufficient stock for ingredient: " + ingredient.getId());
			}
		}
		for (Ingredient ingredient : ingredients) {
			BigDecimal needed = requiredStock.get(ingredient.getId());
			ingredient.setStockQuantity(ingredient.getStockQuantity().subtract(needed));
		}
	}

	private void recalculateAvailabilityForIngredients(Set<Long> ingredientIds) {
		for (Long ingredientId : ingredientIds.stream().sorted().toList()) {
			menuAvailabilityService.recalculateForIngredient(ingredientId);
		}
	}

	private String generateOrderNumber() {
		return UUID.randomUUID().toString();
	}

	private BigDecimal money(BigDecimal value) {
		return value.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
	}

	private OrderResponse toResponse(RestaurantOrder order) {
		List<OrderItemResponse> items = orderItemRepository.findByOrderIdOrderByIdAsc(order.getId()).stream()
				.map(item -> new OrderItemResponse(
						item.getId(),
						item.getMenuItem().getId(),
						item.getMenuItemName(),
						item.getUnitPrice(),
						item.getQuantity(),
						item.getLineTotal()))
				.toList();
		return new OrderResponse(
				order.getId(),
				order.getOrderNumber(),
				order.getDiningTable().getId(),
				order.getDiningTable().getTableNumber(),
				order.getWaiter().getId(),
				order.getWaiter().getFullName(),
				order.getStatus().name(),
				order.isClosed(),
				order.getTotalAmount(),
				order.getCreatedAt(),
				order.getUpdatedAt(),
				items);
	}

	private KitchenOrderResponse toKitchenResponse(RestaurantOrder order) {
		List<KitchenOrderItemResponse> items = orderItemRepository.findByOrderIdOrderByIdAsc(order.getId())
				.stream()
				.map(item -> new KitchenOrderItemResponse(
						item.getId(),
						item.getMenuItem().getId(),
						item.getMenuItemName(),
						item.getQuantity()))
				.toList();
		return new KitchenOrderResponse(
				order.getId(),
				order.getOrderNumber(),
				order.getDiningTable().getId(),
				order.getDiningTable().getTableNumber(),
				order.getStatus().name(),
				order.getCreatedAt(),
				order.getUpdatedAt(),
				items);
	}
}