package bg.martinandonov.restaurant.report.dto;

import java.math.BigDecimal;
import java.util.Objects;

import bg.martinandonov.restaurant.payment.entity.PaymentMethod;

public final class PaymentMethodSalesRowResponse {

	private final PaymentMethod method;
	private final long paymentCount;
	private final BigDecimal amount;
	private final BigDecimal percentageOfRevenue;

	public PaymentMethodSalesRowResponse(
			PaymentMethod method,
			long paymentCount,
			BigDecimal amount,
			BigDecimal percentageOfRevenue) {
		this.method = Objects.requireNonNull(method, "method must not be null");
		this.paymentCount = paymentCount;
		this.amount = Objects.requireNonNull(amount, "amount must not be null");
		this.percentageOfRevenue = Objects.requireNonNull(percentageOfRevenue, "percentageOfRevenue must not be null");
	}

	public PaymentMethod getMethod() {
		return method;
	}

	public long getPaymentCount() {
		return paymentCount;
	}

	public BigDecimal getAmount() {
		return amount;
	}

	public BigDecimal getPercentageOfRevenue() {
		return percentageOfRevenue;
	}
}