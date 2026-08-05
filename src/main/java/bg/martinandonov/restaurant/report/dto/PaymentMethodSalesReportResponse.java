package bg.martinandonov.restaurant.report.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

public final class PaymentMethodSalesReportResponse {

	private final ReportPeriodResponse period;
	private final BigDecimal totalRevenue;
	private final List<PaymentMethodSalesRowResponse> methods;

	public PaymentMethodSalesReportResponse(
			ReportPeriodResponse period,
			BigDecimal totalRevenue,
			List<PaymentMethodSalesRowResponse> methods) {
		this.period = Objects.requireNonNull(period, "period must not be null");
		this.totalRevenue = Objects.requireNonNull(totalRevenue, "totalRevenue must not be null");
		this.methods = List.copyOf(Objects.requireNonNull(methods, "methods must not be null"));
	}

	public ReportPeriodResponse getPeriod() {
		return period;
	}

	public BigDecimal getTotalRevenue() {
		return totalRevenue;
	}

	public List<PaymentMethodSalesRowResponse> getMethods() {
		return methods;
	}
}