package bg.martinandonov.restaurant.report.dto;

import java.util.List;
import java.util.Objects;

public final class MenuItemSalesReportResponse {

	private final ReportPeriodResponse period;
	private final List<MenuItemSalesRowResponse> items;

	public MenuItemSalesReportResponse(ReportPeriodResponse period, List<MenuItemSalesRowResponse> items) {
		this.period = Objects.requireNonNull(period, "period must not be null");
		this.items = List.copyOf(Objects.requireNonNull(items, "items must not be null"));
	}

	public ReportPeriodResponse getPeriod() {
		return period;
	}

	public List<MenuItemSalesRowResponse> getItems() {
		return items;
	}
}