package bg.martinandonov.restaurant.user.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import bg.martinandonov.restaurant.user.entity.AppUser;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {

	Optional<AppUser> findByEmail(String email);

	boolean existsByEmail(String email);

	List<AppUser> findAllByOrderByIdAsc();

	long countByEnabledTrueAndRoles_Name(bg.martinandonov.restaurant.user.entity.RoleName name);
}