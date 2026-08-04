package bg.martinandonov.restaurant.order.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

import bg.martinandonov.restaurant.diningtable.entity.DiningTable;
import bg.martinandonov.restaurant.user.entity.AppUser;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;

@Entity
@Table(
		name = "restaurant_orders",
		uniqueConstraints = @UniqueConstraint(name = "uk_restaurant_orders_order_number", columnNames = "order_number"))
public class RestaurantOrder {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "order_number", nullable = false, length = 36)
	private String orderNumber;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "dining_table_id", nullable = false)
	private DiningTable diningTable;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "waiter_id", nullable = false)
	private AppUser waiter;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 32)
	private OrderStatus status = OrderStatus.ACCEPTED;

	@Column(name = "closed", nullable = false)
	private boolean closed = false;

	@Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
	private BigDecimal totalAmount = BigDecimal.ZERO.setScale(2);

	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt;

	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;

	@Version
	@Column(name = "version", nullable = false)
	private Long version;

	protected RestaurantOrder() {
	}

	public RestaurantOrder(
			String orderNumber,
			DiningTable diningTable,
			AppUser waiter,
			LocalDateTime createdAt) {
		this.orderNumber = Objects.requireNonNull(orderNumber, "orderNumber must not be null");
		this.diningTable = Objects.requireNonNull(diningTable, "diningTable must not be null");
		this.waiter = Objects.requireNonNull(waiter, "waiter must not be null");
		this.status = OrderStatus.ACCEPTED;
		this.closed = false;
		this.totalAmount = BigDecimal.ZERO.setScale(2);
		this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
		this.updatedAt = createdAt;
	}

	public Long getId() {
		return id;
	}

	public String getOrderNumber() {
		return orderNumber;
	}

	public DiningTable getDiningTable() {
		return diningTable;
	}

	public AppUser getWaiter() {
		return waiter;
	}

	public OrderStatus getStatus() {
		return status;
	}

	public void setStatus(OrderStatus status) {
		this.status = Objects.requireNonNull(status, "status must not be null");
	}

	public boolean isClosed() {
		return closed;
	}

	public void setClosed(boolean closed) {
		this.closed = closed;
	}

	public BigDecimal getTotalAmount() {
		return totalAmount;
	}

	public void setTotalAmount(BigDecimal totalAmount) {
		this.totalAmount = Objects.requireNonNull(totalAmount, "totalAmount must not be null");
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public LocalDateTime getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(LocalDateTime updatedAt) {
		this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt must not be null");
	}

	public Long getVersion() {
		return version;
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof RestaurantOrder order)) {
			return false;
		}
		return id != null && Objects.equals(id, order.id);
	}

	@Override
	public int hashCode() {
		return getClass().hashCode();
	}
}
