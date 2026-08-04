package bg.martinandonov.restaurant.payment.dto;

import bg.martinandonov.restaurant.payment.entity.PaymentMethod;

public class ProcessPaymentRequest {

	private PaymentMethod method;

	public PaymentMethod getMethod() {
		return method;
	}

	public void setMethod(PaymentMethod method) {
		this.method = method;
	}
}