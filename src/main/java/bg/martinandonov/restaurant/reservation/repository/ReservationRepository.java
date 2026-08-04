package bg.martinandonov.restaurant.reservation.repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import bg.martinandonov.restaurant.reservation.entity.Reservation;
import bg.martinandonov.restaurant.reservation.entity.ReservationStatus;
import jakarta.persistence.LockModeType;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

	Optional<Reservation> findByReservationNumber(String reservationNumber);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select r from Reservation r where r.id = :id")
	Optional<Reservation> findByIdForUpdate(@Param("id") Long id);

	@Query("""
			select case when count(r) > 0 then true else false end
			from Reservation r
			where r.diningTable.id = :tableId
			  and r.status = bg.martinandonov.restaurant.reservation.entity.ReservationStatus.CONFIRMED
			  and r.startTime < :endTime
			  and r.endTime > :startTime
			""")
	boolean existsConfirmedConflict(
			@Param("tableId") Long tableId,
			@Param("startTime") LocalDateTime startTime,
			@Param("endTime") LocalDateTime endTime);

	@Query("""
			select case when count(r) > 0 then true else false end
			from Reservation r
			where r.diningTable.id = :tableId
			  and r.status = bg.martinandonov.restaurant.reservation.entity.ReservationStatus.CONFIRMED
			  and r.id <> :excludeId
			  and r.startTime < :endTime
			  and r.endTime > :startTime
			""")
	boolean existsConfirmedConflictExcluding(
			@Param("tableId") Long tableId,
			@Param("startTime") LocalDateTime startTime,
			@Param("endTime") LocalDateTime endTime,
			@Param("excludeId") Long excludeId);

	@Query("""
			select r from Reservation r
			join fetch r.diningTable t
			join fetch r.client c
			where r.client.id = :clientId
			order by r.startTime asc, t.tableNumber asc, r.id asc
			""")
	List<Reservation> findByClientIdOrderByStart(@Param("clientId") Long clientId);

	@Query("""
			select r from Reservation r
			join fetch r.diningTable t
			join fetch r.client c
			where r.client.id = :clientId
			  and r.id = :id
			""")
	Optional<Reservation> findByIdAndClientId(@Param("id") Long id, @Param("clientId") Long clientId);

	@Query("""
			select r from Reservation r
			join fetch r.diningTable t
			join fetch r.client c
			where r.id = :id
			""")
	Optional<Reservation> findByIdWithDetails(@Param("id") Long id);

	@Query("""
			select r from Reservation r
			join fetch r.diningTable t
			join fetch r.client c
			where r.startTime < :to
			  and r.endTime > :from
			  and (:tableId is null or t.id = :tableId)
			  and (:clientId is null or c.id = :clientId)
			  and (:statuses is null or r.status in :statuses)
			order by r.startTime asc, t.tableNumber asc, r.id asc
			""")
	List<Reservation> findSchedule(
			@Param("from") LocalDateTime from,
			@Param("to") LocalDateTime to,
			@Param("tableId") Long tableId,
			@Param("clientId") Long clientId,
			@Param("statuses") Collection<ReservationStatus> statuses);

	@Query("""
			select case when count(r) > 0 then true else false end
			from Reservation r
			where r.diningTable.id = :tableId
			  and r.status = bg.martinandonov.restaurant.reservation.entity.ReservationStatus.CONFIRMED
			  and r.startTime > :now
			""")
	boolean existsFutureConfirmedForTable(
			@Param("tableId") Long tableId,
			@Param("now") LocalDateTime now);
}
