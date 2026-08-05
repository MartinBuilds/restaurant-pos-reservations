package bg.martinandonov.restaurant.report.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import bg.martinandonov.restaurant.payment.entity.Payment;
import bg.martinandonov.restaurant.report.projection.MenuItemSalesProjection;
import bg.martinandonov.restaurant.report.projection.PaymentMethodSalesProjection;
import bg.martinandonov.restaurant.report.projection.PaymentSummaryProjection;
import bg.martinandonov.restaurant.report.projection.SoldItemsSummaryProjection;

public interface SalesReportRepository extends Repository<Payment, Long> {

	@Query("""
			select count(p.id) as paymentCount,
			       coalesce(sum(p.amount), 0) as totalAmount
			from Payment p
			where p.paidAt >= :from
			  and p.paidAt < :to
			  and p.order.closed = true
			  and p.order.status = bg.martinandonov.restaurant.order.entity.OrderStatus.SERVED
			""")
	PaymentSummaryProjection summarizePayments(
			@Param("from") LocalDateTime from,
			@Param("to") LocalDateTime to);

	@Query("""
			select coalesce(sum(oi.quantity), 0) as soldItemsCount
			from OrderItem oi, Payment p
			where oi.order = p.order
			  and p.paidAt >= :from
			  and p.paidAt < :to
			  and p.order.closed = true
			  and p.order.status = bg.martinandonov.restaurant.order.entity.OrderStatus.SERVED
			""")
	SoldItemsSummaryProjection summarizeSoldItems(
			@Param("from") LocalDateTime from,
			@Param("to") LocalDateTime to);

	@Query("""
			select oi.menuItem.id as menuItemId,
			       oi.menuItemName as menuItemName,
			       coalesce(sum(oi.quantity), 0) as quantitySold,
			       coalesce(sum(oi.lineTotal), 0) as revenue,
			       count(distinct oi.order.id) as paidOrdersCount
			from OrderItem oi, Payment p
			where oi.order = p.order
			  and p.paidAt >= :from
			  and p.paidAt < :to
			  and p.order.closed = true
			  and p.order.status = bg.martinandonov.restaurant.order.entity.OrderStatus.SERVED
			group by oi.menuItem.id, oi.menuItemName
			order by coalesce(sum(oi.lineTotal), 0) desc,
			         coalesce(sum(oi.quantity), 0) desc,
			         oi.menuItemName asc,
			         oi.menuItem.id asc
			""")
	List<MenuItemSalesProjection> aggregateSalesByMenuItem(
			@Param("from") LocalDateTime from,
			@Param("to") LocalDateTime to);

	@Query("""
			select p.method as method,
			       count(p.id) as paymentCount,
			       coalesce(sum(p.amount), 0) as amount
			from Payment p
			where p.paidAt >= :from
			  and p.paidAt < :to
			  and p.order.closed = true
			  and p.order.status = bg.martinandonov.restaurant.order.entity.OrderStatus.SERVED
			group by p.method
			""")
	List<PaymentMethodSalesProjection> aggregateSalesByPaymentMethod(
			@Param("from") LocalDateTime from,
			@Param("to") LocalDateTime to);
}