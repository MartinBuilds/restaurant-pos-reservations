package bg.martinandonov.restaurant.inventory.entity;

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
		name = "recipe_ingredients",
		uniqueConstraints = @UniqueConstraint(
				name = "uk_recipe_ingredients_menu_item_ingredient",
				columnNames = { "menu_item_id", "ingredient_id" }))
public class RecipeIngredient {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "menu_item_id", nullable = false)
	private MenuItem menuItem;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "ingredient_id", nullable = false)
	private Ingredient ingredient;

	@Column(name = "quantity_required", nullable = false, precision = 12, scale = 3)
	private BigDecimal quantityRequired;

	protected RecipeIngredient() {
	}

	public RecipeIngredient(MenuItem menuItem, Ingredient ingredient, BigDecimal quantityRequired) {
		this.menuItem = Objects.requireNonNull(menuItem, "menuItem must not be null");
		this.ingredient = Objects.requireNonNull(ingredient, "ingredient must not be null");
		this.quantityRequired = Objects.requireNonNull(quantityRequired, "quantityRequired must not be null");
	}

	public Long getId() {
		return id;
	}

	public MenuItem getMenuItem() {
		return menuItem;
	}

	public void setMenuItem(MenuItem menuItem) {
		this.menuItem = Objects.requireNonNull(menuItem, "menuItem must not be null");
	}

	public Ingredient getIngredient() {
		return ingredient;
	}

	public void setIngredient(Ingredient ingredient) {
		this.ingredient = Objects.requireNonNull(ingredient, "ingredient must not be null");
	}

	public BigDecimal getQuantityRequired() {
		return quantityRequired;
	}

	public void setQuantityRequired(BigDecimal quantityRequired) {
		this.quantityRequired = Objects.requireNonNull(quantityRequired, "quantityRequired must not be null");
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof RecipeIngredient recipeIngredient)) {
			return false;
		}
		return id != null && Objects.equals(id, recipeIngredient.id);
	}

	@Override
	public int hashCode() {
		return getClass().hashCode();
	}

	@Override
	public String toString() {
		return "RecipeIngredient{id=" + id + ", quantityRequired=" + quantityRequired + "}";
	}
}