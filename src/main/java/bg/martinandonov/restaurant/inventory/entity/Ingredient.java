package bg.martinandonov.restaurant.inventory.entity;

import java.math.BigDecimal;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;

@Entity
@Table(
		name = "ingredients",
		uniqueConstraints = @UniqueConstraint(name = "uk_ingredients_name", columnNames = "name"))
public class Ingredient {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "name", nullable = false, length = 150)
	private String name;

	@Enumerated(EnumType.STRING)
	@Column(name = "unit", nullable = false, length = 32)
	private IngredientUnit unit;

	@Column(name = "stock_quantity", nullable = false, precision = 12, scale = 3)
	private BigDecimal stockQuantity;

	@Column(name = "minimum_stock_level", nullable = false, precision = 12, scale = 3)
	private BigDecimal minimumStockLevel;

	@Column(name = "active", nullable = false)
	private boolean active = true;

	@Version
	@Column(name = "version", nullable = false)
	private Long version;

	protected Ingredient() {
	}

	public Ingredient(
			String name,
			IngredientUnit unit,
			BigDecimal stockQuantity,
			BigDecimal minimumStockLevel,
			boolean active) {
		this.name = Objects.requireNonNull(name, "name must not be null");
		this.unit = Objects.requireNonNull(unit, "unit must not be null");
		this.stockQuantity = Objects.requireNonNull(stockQuantity, "stockQuantity must not be null");
		this.minimumStockLevel = Objects.requireNonNull(minimumStockLevel, "minimumStockLevel must not be null");
		this.active = active;
	}

	public Long getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = Objects.requireNonNull(name, "name must not be null");
	}

	public IngredientUnit getUnit() {
		return unit;
	}

	public void setUnit(IngredientUnit unit) {
		this.unit = Objects.requireNonNull(unit, "unit must not be null");
	}

	public BigDecimal getStockQuantity() {
		return stockQuantity;
	}

	public void setStockQuantity(BigDecimal stockQuantity) {
		this.stockQuantity = Objects.requireNonNull(stockQuantity, "stockQuantity must not be null");
	}

	public BigDecimal getMinimumStockLevel() {
		return minimumStockLevel;
	}

	public void setMinimumStockLevel(BigDecimal minimumStockLevel) {
		this.minimumStockLevel = Objects.requireNonNull(minimumStockLevel, "minimumStockLevel must not be null");
	}

	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}

	public Long getVersion() {
		return version;
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof Ingredient ingredient)) {
			return false;
		}
		return id != null && Objects.equals(id, ingredient.id);
	}

	@Override
	public int hashCode() {
		return getClass().hashCode();
	}

	@Override
	public String toString() {
		return "Ingredient{id=" + id + ", name='" + name + "', unit=" + unit
				+ ", stockQuantity=" + stockQuantity + ", active=" + active + "}";
	}
}