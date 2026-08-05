package bg.martinandonov.restaurant.report.projection;

import java.math.BigDecimal;

public interface MenuItemSalesProjection {

	Long getMenuItemId();

	String getMenuItemName();

	Long getQuantitySold();

	BigDecimal getRevenue();

	Long getPaidOrdersCount();
}