package bg.martinandonov.restaurant.diningtable.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import bg.martinandonov.restaurant.diningtable.entity.DiningTable;
import bg.martinandonov.restaurant.diningtable.entity.DiningTableStatus;
import jakarta.persistence.LockModeType;

public interface DiningTableRepository extends JpaRepository<DiningTable, Long> {

	Optional<DiningTable> findByTableNumber(Integer tableNumber);

	boolean existsByTableNumber(Integer tableNumber);

	boolean existsByTableNumberAndIdNot(Integer tableNumber, Long id);

	List<DiningTable> findAllByOrderByTableNumberAsc();

	List<DiningTable> findByActiveTrueOrderByTableNumberAsc();

	List<DiningTable> findByActiveTrueAndStatusOrderByTableNumberAsc(DiningTableStatus status);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select t from DiningTable t where t.id = :id")
	Optional<DiningTable> findByIdForUpdate(@Param("id") Long id);
}
