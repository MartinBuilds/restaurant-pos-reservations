package bg.martinandonov.restaurant.report.projection;

import java.math.BigDecimal;

public interface PaymentSummaryProjection {

	Long getPaymentCount();

	BigDecimal getTotalAmount();
}