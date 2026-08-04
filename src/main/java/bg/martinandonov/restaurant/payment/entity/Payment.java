package bg.martinandonov.restaurant.payment.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

import bg.martinandonov.restaurant.order.entity.RestaurantOrder;
import bg.martinandonov.restaurant.user.entity.AppUser;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;

@Entity
@Table(
		name = "payments",
		uniqueConstraints = {
				@UniqueConstraint(name = "uk_payments_receipt_number", columnNames = "receipt_number"),
				@UniqueConstraint(name = "uk_payments_order_id", columnNames = "order_id")
		},
		indexes = {
				@Index(name = "idx_payments_paid_at", columnList = "paid_at"),
				@Index(name = "idx_payments_method_paid_at", columnList = "method, paid_at"),
				@Index(name = "idx_payments_processed_by_paid_at", columnList = "processed_by_id, paid_at")
		})
public class Payment {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "receipt_number", nullable = false, length = 64)
	private String receiptNumber;

	@OneToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "order_id", nullable = false, unique = true)
	private RestaurantOrder order;

	@Enumerated(EnumType.STRING)
	@Column(name = "method", nullable = false, length = 16)
	private PaymentMethod method;

	@Column(name = "amount", nullable = false, precision = 12, scale = 2)
	private BigDecimal amount;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "processed_by_id", nullable = false)
	private AppUser processedBy;

	@Column(name = "paid_at", nullable = false)
	private LocalDateTime paidAt;

	@Version
	@Column(name = "version", nullable = false)
	private Long version;

	protected Payment() {
	}

	public Payment(
			String receiptNumber,
			RestaurantOrder order,
			PaymentMethod method,
			BigDecimal amount,
			AppUser processedBy,
			LocalDateTime paidAt) {
		this.receiptNumber = Objects.requireNonNull(receiptNumber, "receiptNumber must not be null");
		this.order = Objects.requireNonNull(order, "order must not be null");
		this.method = Objects.requireNonNull(method, "method must not be null");
		this.amount = Objects.requireNonNull(amount, "amount must not be null");
		this.processedBy = Objects.requireNonNull(processedBy, "processedBy must not be null");
		this.paidAt = Objects.requireNonNull(paidAt, "paidAt must not be null");
	}

	public Long getId() {
		return id;
	}

	public String getReceiptNumber() {
		return receiptNumber;
	}

	public RestaurantOrder getOrder() {
		return order;
	}

	public PaymentMethod getMethod() {
		return method;
	}

	public BigDecimal getAmount() {
		return amount;
	}

	public AppUser getProcessedBy() {
		return processedBy;
	}

	public LocalDateTime getPaidAt() {
		return paidAt;
	}

	public Long getVersion() {
		return version;
	}
}