package bg.martinandonov.restaurant.menu.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import bg.martinandonov.restaurant.menu.entity.MenuItem;

public interface MenuItemRepository extends JpaRepository<MenuItem, Long> {

	boolean existsByCategoryIdAndNameIgnoreCase(Long categoryId, String name);

	boolean existsByCategoryIdAndNameIgnoreCaseAndIdNot(Long categoryId, String name, Long id);

	List<MenuItem> findAllByOrderByIdAsc();

	List<MenuItem> findByCategoryIdOrderByNameAsc(Long categoryId);

	List<MenuItem> findByActiveTrueAndAvailableTrueOrderByNameAsc();

	List<MenuItem> findByActiveTrueAndAvailableTrueAndCategory_ActiveTrueOrderByNameAsc();
}