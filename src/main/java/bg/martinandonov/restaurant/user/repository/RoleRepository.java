package bg.martinandonov.restaurant.user.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import bg.martinandonov.restaurant.user.entity.Role;
import bg.martinandonov.restaurant.user.entity.RoleName;

public interface RoleRepository extends JpaRepository<Role, Long> {

	Optional<Role> findByName(RoleName name);
}