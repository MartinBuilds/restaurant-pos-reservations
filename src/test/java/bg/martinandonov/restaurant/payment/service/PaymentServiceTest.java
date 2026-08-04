package bg.martinandonov.restaurant.payment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
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
import bg.martinandonov.restaurant.menu.entity.MenuCategory;
import bg.martinandonov.restaurant.menu.entity.MenuItem;
import bg.martinandonov.restaurant.order.entity.OrderItem;
import bg.martinandonov.restaurant.order.entity.OrderStatus;
import bg.martinandonov.restaurant.order.entity.RestaurantOrder;
import bg.martinandonov.restaurant.order.repository.OrderItemRepository;
import bg.martinandonov.restaurant.order.repository.RestaurantOrderRepository;
import bg.martinandonov.restaurant.payment.dto.PaymentResponse;
import bg.martinandonov.restaurant.payment.dto.ProcessPaymentRequest;
import bg.martinandonov.restaurant.payment.entity.Payment;
import bg.martinandonov.restaurant.payment.entity.PaymentMethod;
import bg.martinandonov.restaurant.payment.repository.PaymentRepository;
import bg.martinandonov.restaurant.user.entity.AppUser;
import bg.martinandonov.restaurant.user.entity.Role;
import bg.martinandonov.restaurant.user.entity.RoleName;
import bg.martinandonov.restaurant.user.repository.AppUserRepository;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

	private static final ZoneId ZONE = ZoneId.of("Europe/Sofia");
	private static final Instant FIXED_INSTANT = Instant.parse("2026-08-05T12:00:00Z");
	private static final LocalDateTime NOW = LocalDateTime.ofInstant(FIXED_INSTANT, ZONE);
	private static final String WAITER_EMAIL = "waiter@example.com";

	@Mock
	private PaymentRepository paymentRepository;

	@Mock
	private RestaurantOrderRepository restaurantOrderRepository;

	@Mock
	private OrderItemRepository orderItemRepository;

	@Mock
	private DiningTableRepository diningTableRepository;

	@Mock
	private AppUserRepository appUserRepository;

	private PaymentService paymentService;

	private AppUser waiter;
	private DiningTable table;
	private RestaurantOrder order;
	private MenuItem menuItem;
	private OrderItem orderItem;

	@BeforeEach
	void setUp() {
		Clock clock = Clock.fixed(FIXED_INSTANT, ZONE);
		paymentService = new PaymentService(
				paymentRepository,
				restaurantOrderRepository,
				orderItemRepository,
				diningTableRepository,
				appUserRepository,
				clock);

		waiter = new AppUser(WAITER_EMAIL, "hash", "Waiter One", true);
		ReflectionTestUtils.setField(waiter, "id", 5L);
		waiter.setRoles(Set.of(new Role(RoleName.WAITER)));

		table = new DiningTable(7, "Patio", 4);
		ReflectionTestUtils.setField(table, "id", 3L);
		table.setStatus(DiningTableStatus.OCCUPIED);

		order = new RestaurantOrder("ORD-1", table, waiter, NOW.minusHours(1));
		ReflectionTestUtils.setField(order, "id", 10L);
		order.setStatus(OrderStatus.SERVED);
		order.setClosed(false);
		order.setTotalAmount(new BigDecimal("25.50"));

		MenuCategory category = new MenuCategory("Mains", null, true);
		ReflectionTestUtils.setField(category, "id", 1L);
		menuItem = new MenuItem("Soup", null, new BigDecimal("12.75"), true, true, category);
		ReflectionTestUtils.setField(menuItem, "id", 20L);
		orderItem = new OrderItem(
				order, menuItem, "Soup Snapshot", new BigDecimal("12.75"), 2, new BigDecimal("25.50"));
		ReflectionTestUtils.setField(orderItem, "id", 100L);

		SecurityContext context = SecurityContextHolder.createEmptyContext();
		Authentication authentication = mock(Authentication.class);
		lenient().when(authentication.isAuthenticated()).thenReturn(true);
		lenient().when(authentication.getName()).thenReturn(WAITER_EMAIL);
		context.setAuthentication(authentication);
		SecurityContextHolder.setContext(context);
		lenient().when(appUserRepository.findByEmail(WAITER_EMAIL)).thenReturn(Optional.of(waiter));
	}

	@AfterEach
	void tearDown() {
		SecurityContextHolder.clearContext();
	}

	@Test
	void cashPaymentClosesOrderAndFreesTable() {
		stubLocks();
		when(paymentRepository.existsByOrderId(10L)).thenReturn(false);
		when(paymentRepository.saveAndFlush(any(Payment.class))).thenAnswer(invocation -> {
			Payment payment = invocation.getArgument(0);
			ReflectionTestUtils.setField(payment, "id", 50L);
			return payment;
		});
		when(orderItemRepository.findByOrderIdOrderByIdAsc(10L)).thenReturn(List.of(orderItem));

		PaymentResponse response = paymentService.processPayment(10L, request(PaymentMethod.CASH));

		ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
		verify(paymentRepository).saveAndFlush(captor.capture());
		assertThat(captor.getValue().getMethod()).isEqualTo(PaymentMethod.CASH);
		assertThat(captor.getValue().getAmount()).isEqualByComparingTo("25.50");
		assertThat(captor.getValue().getProcessedBy().getId()).isEqualTo(5L);
		assertThat(captor.getValue().getPaidAt()).isEqualTo(NOW);
		assertThat(captor.getValue().getReceiptNumber()).startsWith("SIM-");
		assertThat(order.isClosed()).isTrue();
		assertThat(order.getStatus()).isEqualTo(OrderStatus.SERVED);
		assertThat(table.getStatus()).isEqualTo(DiningTableStatus.AVAILABLE);
		assertThat(response.isSimulated()).isTrue();
		assertThat(response.getMethod()).isEqualTo("CASH");
		assertThat(response.getItems()).hasSize(1);
		assertThat(response.getItems().get(0).getMenuItemName()).isEqualTo("Soup Snapshot");
	}

	@Test
	void cardPaymentStoresCardMethodOnly() {
		stubLocks();
		when(paymentRepository.existsByOrderId(10L)).thenReturn(false);
		when(paymentRepository.saveAndFlush(any(Payment.class))).thenAnswer(invocation -> {
			Payment payment = invocation.getArgument(0);
			ReflectionTestUtils.setField(payment, "id", 51L);
			return payment;
		});
		when(orderItemRepository.findByOrderIdOrderByIdAsc(10L)).thenReturn(List.of(orderItem));

		PaymentResponse response = paymentService.processPayment(10L, request(PaymentMethod.CARD));

		assertThat(response.getMethod()).isEqualTo("CARD");
		assertThat(response.isSimulated()).isTrue();
	}

	@Test
	void nullMethodRejected() {
		assertThatThrownBy(() -> paymentService.processPayment(10L, request(null)))
				.isInstanceOf(InvalidRequestException.class)
				.hasMessageContaining("method");
		verify(diningTableRepository, never()).findByIdForUpdate(any());
	}

	@Test
	void missingOrderReturns404() {
		when(restaurantOrderRepository.findDiningTableIdByOrderId(10L)).thenReturn(Optional.empty());
		assertThatThrownBy(() -> paymentService.processPayment(10L, request(PaymentMethod.CASH)))
				.isInstanceOf(ResourceNotFoundException.class);
	}

	@Test
	void nonServedStatusesRejected() {
		for (OrderStatus status : List.of(
				OrderStatus.ACCEPTED, OrderStatus.COOKING, OrderStatus.READY, OrderStatus.CANCELLED)) {
			order.setStatus(status);
			order.setClosed(false);
			table.setStatus(DiningTableStatus.OCCUPIED);
			stubLocks();
			assertThatThrownBy(() -> paymentService.processPayment(10L, request(PaymentMethod.CASH)))
					.isInstanceOf(BusinessRuleException.class)
					.hasMessageContaining("SERVED");
		}
	}

	@Test
	void closedOrExistingPaymentRejected() {
		stubLocks();
		order.setClosed(true);
		assertThatThrownBy(() -> paymentService.processPayment(10L, request(PaymentMethod.CASH)))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("closed");

		order.setClosed(false);
		stubLocks();
		when(paymentRepository.existsByOrderId(10L)).thenReturn(true);
		assertThatThrownBy(() -> paymentService.processPayment(10L, request(PaymentMethod.CASH)))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("already has a payment");
	}

	@Test
	void invalidAmountAndTableStateRejected() {
		stubLocks();
		order.setTotalAmount(BigDecimal.ZERO);
		assertThatThrownBy(() -> paymentService.processPayment(10L, request(PaymentMethod.CASH)))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("total");

		order.setTotalAmount(new BigDecimal("25.50"));
		table.setActive(false);
		stubLocks();
		assertThatThrownBy(() -> paymentService.processPayment(10L, request(PaymentMethod.CASH)))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("inactive");

		table.setActive(true);
		table.setStatus(DiningTableStatus.AVAILABLE);
		stubLocks();
		assertThatThrownBy(() -> paymentService.processPayment(10L, request(PaymentMethod.CASH)))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("OCCUPIED");
	}

	@Test
	void cookAndClientDenied() {
		AppUser cook = new AppUser(WAITER_EMAIL, "hash", "Cook", true);
		ReflectionTestUtils.setField(cook, "id", 8L);
		cook.setRoles(Set.of(new Role(RoleName.COOK)));
		when(appUserRepository.findByEmail(WAITER_EMAIL)).thenReturn(Optional.of(cook));
		assertThatThrownBy(() -> paymentService.processPayment(10L, request(PaymentMethod.CASH)))
				.isInstanceOf(AccessDeniedException.class);

		AppUser client = new AppUser(WAITER_EMAIL, "hash", "Client", true);
		ReflectionTestUtils.setField(client, "id", 9L);
		client.setRoles(Set.of(new Role(RoleName.CLIENT)));
		when(appUserRepository.findByEmail(WAITER_EMAIL)).thenReturn(Optional.of(client));
		assertThatThrownBy(() -> paymentService.processPayment(10L, request(PaymentMethod.CASH)))
				.isInstanceOf(AccessDeniedException.class);
	}

	@Test
	void disabledUserDenied() {
		waiter.setEnabled(false);
		assertThatThrownBy(() -> paymentService.processPayment(10L, request(PaymentMethod.CASH)))
				.isInstanceOf(AccessDeniedException.class);
	}

	@Test
	void uniqueConstraintBecomesConflictAndLeavesState() {
		stubLocks();
		when(paymentRepository.existsByOrderId(10L)).thenReturn(false);
		when(paymentRepository.saveAndFlush(any(Payment.class)))
				.thenThrow(new DataIntegrityViolationException("uk_payments_order_id"));

		assertThatThrownBy(() -> paymentService.processPayment(10L, request(PaymentMethod.CASH)))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("already has a payment");
		assertThat(order.isClosed()).isFalse();
		assertThat(table.getStatus()).isEqualTo(DiningTableStatus.OCCUPIED);
	}

	@Test
	void getPaymentForOrderMissingReturns404() {
		when(restaurantOrderRepository.existsById(10L)).thenReturn(true);
		when(paymentRepository.findByOrderIdWithDetails(10L)).thenReturn(Optional.empty());
		assertThatThrownBy(() -> paymentService.getPaymentForOrder(10L))
				.isInstanceOf(ResourceNotFoundException.class);
	}

	@Test
	void adminListRejectsInvalidPeriod() {
		assertThatThrownBy(() -> paymentService.getPaymentsForAdmin(
				null, NOW.plusHours(1), NOW, null))
				.isInstanceOf(InvalidRequestException.class)
				.hasMessageContaining("from");
	}

	private void stubLocks() {
		when(restaurantOrderRepository.findDiningTableIdByOrderId(10L)).thenReturn(Optional.of(3L));
		when(diningTableRepository.findByIdForUpdate(3L)).thenReturn(Optional.of(table));
		when(restaurantOrderRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(order));
	}

	private ProcessPaymentRequest request(PaymentMethod method) {
		ProcessPaymentRequest request = new ProcessPaymentRequest();
		request.setMethod(method);
		return request;
	}
}