package bg.martinandonov.restaurant.order.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import bg.martinandonov.restaurant.order.entity.OrderStatus;
import bg.martinandonov.restaurant.order.entity.RestaurantOrder;
import jakarta.persistence.LockModeType;

public interface RestaurantOrderRepository extends JpaRepository<RestaurantOrder, Long> {

	Optional<RestaurantOrder> findByOrderNumber(String orderNumber);

	boolean existsByDiningTableIdAndClosedFalse(Long diningTableId);

	List<RestaurantOrder> findByClosedFalseOrderByCreatedAtAscIdAsc();

	@Query("""
			select distinct o from RestaurantOrder o
			join fetch o.diningTable
			join fetch o.waiter
			where o.closed = false
			order by o.createdAt asc, o.id asc
			""")
	List<RestaurantOrder> findOpenOrdersWithDetails();

	List<RestaurantOrder> findByDiningTableIdAndClosedFalseOrderByCreatedAtAscIdAsc(Long diningTableId);

	@Query("""
			select distinct o from RestaurantOrder o
			join fetch o.diningTable
			join fetch o.waiter
			where o.diningTable.id = :tableId and o.closed = false
			order by o.createdAt asc, o.id asc
			""")
	List<RestaurantOrder> findOpenOrdersByTableWithDetails(@Param("tableId") Long tableId);

	List<RestaurantOrder> findByDiningTableIdOrderByCreatedAtAscIdAsc(Long diningTableId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("""
			select o from RestaurantOrder o
			join fetch o.diningTable
			join fetch o.waiter
			where o.id = :id
			""")
	Optional<RestaurantOrder> findByIdForUpdate(@Param("id") Long id);

	@Query("""
			select o from RestaurantOrder o
			join fetch o.diningTable
			join fetch o.waiter
			where o.id = :id
			""")
	Optional<RestaurantOrder> findByIdWithDetails(@Param("id") Long id);

	@Query("""
			select distinct o from RestaurantOrder o
			join fetch o.diningTable
			where o.closed = false
			and o.status in :statuses
			order by o.createdAt asc, o.id asc
			""")
	List<RestaurantOrder> findActiveKitchenOrders(@Param("statuses") Collection<OrderStatus> statuses);

	@Query("""
			select distinct o from RestaurantOrder o
			join fetch o.diningTable
			where o.closed = false
			and o.status = :status
			order by o.createdAt asc, o.id asc
			""")
	List<RestaurantOrder> findActiveKitchenOrdersByStatus(@Param("status") OrderStatus status);
}
