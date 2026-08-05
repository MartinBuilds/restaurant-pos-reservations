package bg.martinandonov.restaurant.report.dto;

import java.math.BigDecimal;
import java.util.Objects;

public final class SalesSummaryResponse {

	private final ReportPeriodResponse period;
	private final BigDecimal totalRevenue;
	private final long paidOrdersCount;
	private final long soldItemsCount;
	private final BigDecimal averageOrderValue;

	public SalesSummaryResponse(
			ReportPeriodResponse period,
			BigDecimal totalRevenue,
			long paidOrdersCount,
			long soldItemsCount,
			BigDecimal averageOrderValue) {
		this.period = Objects.requireNonNull(period, "period must not be null");
		this.totalRevenue = Objects.requireNonNull(totalRevenue, "totalRevenue must not be null");
		this.paidOrdersCount = paidOrdersCount;
		this.soldItemsCount = soldItemsCount;
		this.averageOrderValue = Objects.requireNonNull(averageOrderValue, "averageOrderValue must not be null");
	}

	public ReportPeriodResponse getPeriod() {
		return period;
	}

	public BigDecimal getTotalRevenue() {
		return totalRevenue;
	}

	public long getPaidOrdersCount() {
		return paidOrdersCount;
	}

	public long getSoldItemsCount() {
		return soldItemsCount;
	}

	public BigDecimal getAverageOrderValue() {
		return averageOrderValue;
	}
}