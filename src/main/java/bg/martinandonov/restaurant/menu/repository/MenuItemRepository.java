package bg.martinandonov.restaurant.menu.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import bg.martinandonov.restaurant.menu.entity.MenuItem;

public interface MenuItemRepository extends JpaRepository<MenuItem, Long> {

	boolean existsByCategoryIdAndNameIgnoreCase(Long categoryId, String name);

	boolean existsByCategoryIdAndNameIgnoreCaseAndIdNot(Long categoryId, String name, Long id);

	List<MenuItem> findAllByOrderByIdAsc();

	List<MenuItem> findByCategoryIdOrderByNameAsc(Long categoryId);

	List<MenuItem> findByActiveTrueAndAvailableTrueOrderByNameAsc();

	List<MenuItem> findByActiveTrueAndAvailableTrueAndCategory_ActiveTrueOrderByNameAsc();

	@Query("""
			select m from MenuItem m
			join fetch m.category
			where m.id = :id
			""")
	Optional<MenuItem> findByIdWithCategory(@Param("id") Long id);

	@Query("""
			select distinct m from MenuItem m
			join fetch m.category
			where m.id in :ids
			order by m.id asc
			""")
	List<MenuItem> findAllByIdInWithCategoryOrderByIdAsc(@Param("ids") Collection<Long> ids);
}