package bg.martinandonov.restaurant.menu.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import bg.martinandonov.restaurant.menu.entity.MenuCategory;

public interface MenuCategoryRepository extends JpaRepository<MenuCategory, Long> {

	Optional<MenuCategory> findByNameIgnoreCase(String name);

	boolean existsByNameIgnoreCase(String name);

	boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

	List<MenuCategory> findAllByOrderByNameAsc();

	List<MenuCategory> findByActiveTrueOrderByNameAsc();
}