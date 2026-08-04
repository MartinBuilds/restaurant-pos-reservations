package bg.martinandonov.restaurant.inventory.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import bg.martinandonov.restaurant.inventory.entity.Ingredient;
import jakarta.persistence.LockModeType;

public interface IngredientRepository extends JpaRepository<Ingredient, Long> {

	Optional<Ingredient> findByNameIgnoreCase(String name);

	boolean existsByNameIgnoreCase(String name);

	boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

	List<Ingredient> findAllByOrderByNameAsc();

	List<Ingredient> findByActiveTrueOrderByNameAsc();

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select i from Ingredient i where i.id = :id")
	Optional<Ingredient> findByIdForUpdate(@Param("id") Long id);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select i from Ingredient i where i.id in :ids order by i.id asc")
	List<Ingredient> findAllByIdInOrderByIdAscForUpdate(@Param("ids") Collection<Long> ids);
}