package bg.martinandonov.restaurant.payment.service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
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
import bg.martinandonov.restaurant.order.entity.OrderItem;
import bg.martinandonov.restaurant.order.entity.OrderStatus;
import bg.martinandonov.restaurant.order.entity.RestaurantOrder;
import bg.martinandonov.restaurant.order.repository.OrderItemRepository;
import bg.martinandonov.restaurant.order.repository.RestaurantOrderRepository;
import bg.martinandonov.restaurant.payment.dto.PaymentReceiptItemResponse;
import bg.martinandonov.restaurant.payment.dto.PaymentResponse;
import bg.martinandonov.restaurant.payment.dto.ProcessPaymentRequest;
import bg.martinandonov.restaurant.payment.entity.Payment;
import bg.martinandonov.restaurant.payment.entity.PaymentMethod;
import bg.martinandonov.restaurant.payment.repository.PaymentRepository;
import bg.martinandonov.restaurant.user.entity.AppUser;
import bg.martinandonov.restaurant.user.entity.RoleName;
import bg.martinandonov.restaurant.user.repository.AppUserRepository;

@Service
public class PaymentService {

	private final PaymentRepository paymentRepository;
	private final RestaurantOrderRepository restaurantOrderRepository;
	private final OrderItemRepository orderItemRepository;
	private final DiningTableRepository diningTableRepository;
	private final AppUserRepository appUserRepository;
	private final Clock clock;

	public PaymentService(
			PaymentRepository paymentRepository,
			RestaurantOrderRepository restaurantOrderRepository,
			OrderItemRepository orderItemRepository,
			DiningTableRepository diningTableRepository,
			AppUserRepository appUserRepository,
			Clock clock) {
		this.paymentRepository = paymentRepository;
		this.restaurantOrderRepository = restaurantOrderRepository;
		this.orderItemRepository = orderItemRepository;
		this.diningTableRepository = diningTableRepository;
		this.appUserRepository = appUserRepository;
		this.clock = clock;
	}

	@Transactional
	public PaymentResponse processPayment(Long orderId, ProcessPaymentRequest request) {
		PaymentMethod method = requireMethod(request);
		AppUser operator = requireAuthenticatedWaiterOrAdmin();
		Long id = requireId(orderId);

		Long diningTableId = restaurantOrderRepository.findDiningTableIdByOrderId(id)
				.orElseThrow(() -> new ResourceNotFoundException("Order not found: " + id));

		DiningTable table = diningTableRepository.findByIdForUpdate(diningTableId)
				.orElseThrow(() -> new ResourceNotFoundException("Dining table not found: " + diningTableId));

		RestaurantOrder order = restaurantOrderRepository.findByIdForUpdate(id)
				.orElseThrow(() -> new ResourceNotFoundException("Order not found: " + id));

		if (!Objects.equals(order.getDiningTable().getId(), table.getId())) {
			throw new BusinessRuleException("Order does not belong to the locked dining table");
		}

		assertPayable(order, table);

		if (paymentRepository.existsByOrderId(order.getId())) {
			throw new BusinessRuleException("Order already has a payment");
		}

		BigDecimal amount = order.getTotalAmount();
		LocalDateTime paidAt = LocalDateTime.now(clock);
		Payment payment = new Payment(
				generateReceiptNumber(),
				order,
				method,
				amount,
				operator,
				paidAt);

		try {
			payment = paymentRepository.saveAndFlush(payment);
		}
		catch (DataIntegrityViolationException ex) {
			throw new BusinessRuleException("Order already has a payment");
		}

		order.setClosed(true);
		order.setUpdatedAt(paidAt);
		table.setStatus(DiningTableStatus.AVAILABLE);

		return toResponse(payment, orderItemRepository.findByOrderIdOrderByIdAsc(order.getId()));
	}

	@Transactional(readOnly = true)
	public PaymentResponse getPaymentForOrder(Long orderId) {
		Long id = requireId(orderId);
		if (!restaurantOrderRepository.existsById(id)) {
			throw new ResourceNotFoundException("Order not found: " + id);
		}
		Payment payment = paymentRepository.findByOrderIdWithDetails(id)
				.orElseThrow(() -> new ResourceNotFoundException("Payment not found for order: " + id));
		return toResponse(payment, orderItemRepository.findByOrderIdOrderByIdAsc(id));
	}

	@Transactional(readOnly = true)
	public PaymentResponse getPaymentByIdForAdmin(Long paymentId) {
		Payment payment = paymentRepository.findByIdWithDetails(requireId(paymentId))
				.orElseThrow(() -> new ResourceNotFoundException("Payment not found: " + paymentId));
		return toResponse(
				payment,
				orderItemRepository.findByOrderIdOrderByIdAsc(payment.getOrder().getId()));
	}

	@Transactional(readOnly = true)
	public List<PaymentResponse> getPaymentsForAdmin(
			PaymentMethod method,
			LocalDateTime from,
			LocalDateTime to,
			Long processedById) {
		if (from != null && to != null && !from.isBefore(to)) {
			throw new InvalidRequestException("from must be before to");
		}
		return paymentRepository.findFiltered(method, from, to, processedById).stream()
				.map(payment -> toResponse(
						payment,
						orderItemRepository.findByOrderIdOrderByIdAsc(payment.getOrder().getId())))
				.toList();
	}

	private void assertPayable(RestaurantOrder order, DiningTable table) {
		if (order.getStatus() != OrderStatus.SERVED) {
			throw new BusinessRuleException("Only SERVED orders can be paid");
		}
		if (order.isClosed()) {
			throw new BusinessRuleException("Order is already closed");
		}
		if (order.getTotalAmount() == null || order.getTotalAmount().compareTo(BigDecimal.ZERO) <= 0) {
			throw new BusinessRuleException("Order total amount must be greater than zero");
		}
		if (!table.isActive()) {
			throw new BusinessRuleException("Dining table is inactive");
		}
		if (table.getStatus() != DiningTableStatus.OCCUPIED) {
			throw new BusinessRuleException("Dining table must be OCCUPIED to accept payment");
		}
	}

	private PaymentMethod requireMethod(ProcessPaymentRequest request) {
		if (request == null || request.getMethod() == null) {
			throw new InvalidRequestException("method must be provided");
		}
		return request.getMethod();
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
			throw new AccessDeniedException("Only WAITER or ADMIN can process payments");
		}
		return user;
	}

	private Long requireId(Long id) {
		if (id == null) {
			throw new InvalidRequestException("id must be provided");
		}
		return id;
	}

	private String generateReceiptNumber() {
		return "SIM-" + UUID.randomUUID();
	}

	private PaymentResponse toResponse(Payment payment, List<OrderItem> items) {
		RestaurantOrder order = payment.getOrder();
		DiningTable table = order.getDiningTable();
		AppUser processedBy = payment.getProcessedBy();
		List<PaymentReceiptItemResponse> receiptItems = items.stream()
				.map(item -> new PaymentReceiptItemResponse(
						item.getId(),
						item.getMenuItem().getId(),
						item.getMenuItemName(),
						item.getUnitPrice(),
						item.getQuantity(),
						item.getLineTotal()))
				.toList();
		return new PaymentResponse(
				payment.getId(),
				payment.getReceiptNumber(),
				true,
				order.getId(),
				order.getOrderNumber(),
				table.getId(),
				table.getTableNumber(),
				order.getStatus().name(),
				order.isClosed(),
				payment.getMethod().name(),
				payment.getAmount(),
				processedBy.getId(),
				processedBy.getFullName(),
				payment.getPaidAt(),
				receiptItems);
	}

	public static PaymentMethod parseOptionalMethod(String method) {
		if (method == null || method.isBlank()) {
			return null;
		}
		try {
			return PaymentMethod.valueOf(method.trim().toUpperCase(Locale.ROOT));
		}
		catch (IllegalArgumentException ex) {
			throw new InvalidRequestException("Unknown payment method: " + method.trim());
		}
	}
}