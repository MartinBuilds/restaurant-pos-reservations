package bg.martinandonov.restaurant.order.entity;

import java.math.BigDecimal;
import java.util.Objects;

import bg.martinandonov.restaurant.menu.entity.MenuItem;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
		name = "order_items",
		uniqueConstraints = @UniqueConstraint(
				name = "uk_order_items_order_menu_item",
				columnNames = { "order_id", "menu_item_id" }))
public class OrderItem {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "order_id", nullable = false)
	private RestaurantOrder order;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "menu_item_id", nullable = false)
	private MenuItem menuItem;

	@Column(name = "menu_item_name", nullable = false, length = 150)
	private String menuItemName;

	@Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
	private BigDecimal unitPrice;

	@Column(name = "quantity", nullable = false)
	private Integer quantity;

	@Column(name = "line_total", nullable = false, precision = 12, scale = 2)
	private BigDecimal lineTotal;

	protected OrderItem() {
	}

	public OrderItem(
			RestaurantOrder order,
			MenuItem menuItem,
			String menuItemName,
			BigDecimal unitPrice,
			Integer quantity,
			BigDecimal lineTotal) {
		this.order = Objects.requireNonNull(order, "order must not be null");
		this.menuItem = Objects.requireNonNull(menuItem, "menuItem must not be null");
		this.menuItemName = Objects.requireNonNull(menuItemName, "menuItemName must not be null");
		this.unitPrice = Objects.requireNonNull(unitPrice, "unitPrice must not be null");
		this.quantity = Objects.requireNonNull(quantity, "quantity must not be null");
		this.lineTotal = Objects.requireNonNull(lineTotal, "lineTotal must not be null");
	}

	public Long getId() {
		return id;
	}

	public RestaurantOrder getOrder() {
		return order;
	}

	public MenuItem getMenuItem() {
		return menuItem;
	}

	public String getMenuItemName() {
		return menuItemName;
	}

	public BigDecimal getUnitPrice() {
		return unitPrice;
	}

	public Integer getQuantity() {
		return quantity;
	}

	public void setQuantity(Integer quantity) {
		this.quantity = Objects.requireNonNull(quantity, "quantity must not be null");
	}

	public BigDecimal getLineTotal() {
		return lineTotal;
	}

	public void setLineTotal(BigDecimal lineTotal) {
		this.lineTotal = Objects.requireNonNull(lineTotal, "lineTotal must not be null");
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof OrderItem item)) {
			return false;
		}
		return id != null && Objects.equals(id, item.id);
	}

	@Override
	public int hashCode() {
		return getClass().hashCode();
	}
}
