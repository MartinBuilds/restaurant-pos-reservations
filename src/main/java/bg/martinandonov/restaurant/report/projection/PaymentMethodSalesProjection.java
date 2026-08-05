package bg.martinandonov.restaurant.report.projection;

import java.math.BigDecimal;

import bg.martinandonov.restaurant.payment.entity.PaymentMethod;

public interface PaymentMethodSalesProjection {

	PaymentMethod getMethod();

	Long getPaymentCount();

	BigDecimal getAmount();
}