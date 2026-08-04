package bg.martinandonov.restaurant.order.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import bg.martinandonov.restaurant.order.entity.OrderItem;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

	@Query("""
			select oi from OrderItem oi
			join fetch oi.menuItem
			where oi.order.id = :orderId
			order by oi.id asc
			""")
	List<OrderItem> findByOrderIdOrderByIdAsc(@Param("orderId") Long orderId);

	boolean existsByOrderIdAndMenuItemId(Long orderId, Long menuItemId);

	Optional<OrderItem> findByOrderIdAndMenuItemId(Long orderId, Long menuItemId);
}
