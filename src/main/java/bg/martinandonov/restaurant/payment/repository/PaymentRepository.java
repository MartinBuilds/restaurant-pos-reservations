package bg.martinandonov.restaurant.payment.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import bg.martinandonov.restaurant.payment.entity.Payment;
import bg.martinandonov.restaurant.payment.entity.PaymentMethod;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

	Optional<Payment> findByReceiptNumber(String receiptNumber);

	Optional<Payment> findByOrderId(Long orderId);

	boolean existsByOrderId(Long orderId);

	@Query("""
			select p from Payment p
			join fetch p.order o
			join fetch o.diningTable
			join fetch p.processedBy
			where p.id = :id
			""")
	Optional<Payment> findByIdWithDetails(@Param("id") Long id);

	@Query("""
			select p from Payment p
			join fetch p.order o
			join fetch o.diningTable
			join fetch p.processedBy
			where o.id = :orderId
			""")
	Optional<Payment> findByOrderIdWithDetails(@Param("orderId") Long orderId);

	@Query("""
			select p from Payment p
			join fetch p.order o
			join fetch o.diningTable
			join fetch p.processedBy
			where (:method is null or p.method = :method)
			  and (:from is null or p.paidAt >= :from)
			  and (:to is null or p.paidAt <= :to)
			  and (:processedById is null or p.processedBy.id = :processedById)
			order by p.paidAt desc, p.id desc
			""")
	List<Payment> findFiltered(
			@Param("method") PaymentMethod method,
			@Param("from") LocalDateTime from,
			@Param("to") LocalDateTime to,
			@Param("processedById") Long processedById);
}