package bg.martinandonov.restaurant.diningtable.service;

import org.springframework.stereotype.Component;

import bg.martinandonov.restaurant.common.exception.BusinessRuleException;
import bg.martinandonov.restaurant.order.repository.RestaurantOrderRepository;

@Component
public class DiningTableOperationalGuard {

	private final RestaurantOrderRepository restaurantOrderRepository;

	public DiningTableOperationalGuard(RestaurantOrderRepository restaurantOrderRepository) {
		this.restaurantOrderRepository = restaurantOrderRepository;
	}

	public boolean hasOpenOrder(Long diningTableId) {
		return restaurantOrderRepository.existsByDiningTableIdAndClosedFalse(diningTableId);
	}

	public void assertNoOpenOrder(Long diningTableId) {
		if (hasOpenOrder(diningTableId)) {
			throw new BusinessRuleException("Cannot change a table that has an open order");
		}
	}
}
