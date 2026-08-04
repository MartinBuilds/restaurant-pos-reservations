package bg.martinandonov.restaurant.inventory.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import bg.martinandonov.restaurant.inventory.entity.RecipeIngredient;

public interface RecipeIngredientRepository extends JpaRepository<RecipeIngredient, Long> {

	@Query("""
			select ri from RecipeIngredient ri
			join fetch ri.ingredient i
			join fetch ri.menuItem m
			where m.id = :menuItemId
			order by i.name asc
			""")
	List<RecipeIngredient> findByMenuItemIdOrderByIngredientNameAsc(@Param("menuItemId") Long menuItemId);

	@Query("""
			select distinct m.id from RecipeIngredient ri
			join ri.menuItem m
			join ri.ingredient i
			where i.id = :ingredientId
			order by m.id asc
			""")
	List<Long> findDistinctMenuItemIdsByIngredientIdOrderByMenuItemIdAsc(@Param("ingredientId") Long ingredientId);

	boolean existsByIngredientId(Long ingredientId);

	void deleteByMenuItemId(Long menuItemId);
}
