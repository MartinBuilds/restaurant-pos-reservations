package bg.martinandonov.restaurant.report.dto;

import java.math.BigDecimal;
import java.util.Objects;

public final class MenuItemSalesRowResponse {

	private final Long menuItemId;
	private final String menuItemName;
	private final long quantitySold;
	private final BigDecimal revenue;
	private final long paidOrdersCount;

	public MenuItemSalesRowResponse(
			Long menuItemId,
			String menuItemName,
			long quantitySold,
			BigDecimal revenue,
			long paidOrdersCount) {
		this.menuItemId = Objects.requireNonNull(menuItemId, "menuItemId must not be null");
		this.menuItemName = Objects.requireNonNull(menuItemName, "menuItemName must not be null");
		this.quantitySold = quantitySold;
		this.revenue = Objects.requireNonNull(revenue, "revenue must not be null");
		this.paidOrdersCount = paidOrdersCount;
	}

	public Long getMenuItemId() {
		return menuItemId;
	}

	public String getMenuItemName() {
		return menuItemName;
	}

	public long getQuantitySold() {
		return quantitySold;
	}

	public BigDecimal getRevenue() {
		return revenue;
	}

	public long getPaidOrdersCount() {
		return paidOrdersCount;
	}
}