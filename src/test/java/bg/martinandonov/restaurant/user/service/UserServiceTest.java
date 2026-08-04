package bg.martinandonov.restaurant.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import bg.martinandonov.restaurant.common.exception.BusinessRuleException;
import bg.martinandonov.restaurant.common.exception.InvalidRequestException;
import bg.martinandonov.restaurant.common.exception.ResourceNotFoundException;
import bg.martinandonov.restaurant.user.dto.CreateUserRequest;
import bg.martinandonov.restaurant.user.dto.UpdateUserStatusRequest;
import bg.martinandonov.restaurant.user.dto.UserResponse;
import bg.martinandonov.restaurant.user.entity.AppUser;
import bg.martinandonov.restaurant.user.entity.Role;
import bg.martinandonov.restaurant.user.entity.RoleName;
import bg.martinandonov.restaurant.user.repository.AppUserRepository;
import bg.martinandonov.restaurant.user.repository.RoleRepository;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

	@Mock
	private AppUserRepository appUserRepository;

	@Mock
	private RoleRepository roleRepository;

	@Mock
	private PasswordEncoder passwordEncoder;

	@InjectMocks
	private UserService userService;

	private Role adminRole;
	private Role waiterRole;

	@BeforeEach
	void setUp() {
		adminRole = new Role(RoleName.ADMIN);
		ReflectionTestUtils.setField(adminRole, "id", 1L);
		waiterRole = new Role(RoleName.WAITER);
		ReflectionTestUtils.setField(waiterRole, "id", 2L);
	}

	@Test
	void createUserNormalizesEmailAndHashesPassword() {
		CreateUserRequest request = new CreateUserRequest();
		request.setEmail("  Admin@Example.COM ");
		request.setPassword("password123");
		request.setFullName("Admin User");
		request.setRoles(Set.of("ADMIN"));

		when(appUserRepository.existsByEmail("admin@example.com")).thenReturn(false);
		when(roleRepository.findByName(RoleName.ADMIN)).thenReturn(Optional.of(adminRole));
		when(passwordEncoder.encode("password123")).thenReturn("bcrypt-hash");
		when(appUserRepository.save(any(AppUser.class))).thenAnswer(invocation -> {
			AppUser user = invocation.getArgument(0);
			ReflectionTestUtils.setField(user, "id", 10L);
			return user;
		});

		UserResponse response = userService.createUser(request);

		ArgumentCaptor<AppUser> captor = ArgumentCaptor.forClass(AppUser.class);
		verify(appUserRepository).save(captor.capture());
		AppUser saved = captor.getValue();

		assertThat(saved.getEmail()).isEqualTo("admin@example.com");
		assertThat(saved.getPassword()).isEqualTo("bcrypt-hash");
		assertThat(saved.getPassword()).isNotEqualTo("password123");
		assertThat(response.getEmail()).isEqualTo("admin@example.com");
		assertThat(response.getRoles()).containsExactly("ADMIN");
		assertThat(response.getClass().getDeclaredFields())
				.extracting(field -> field.getName())
				.doesNotContain("password");
	}

	@Test
	void createUserRejectsDuplicateEmail() {
		CreateUserRequest request = new CreateUserRequest();
		request.setEmail("admin@example.com");
		request.setPassword("password123");
		request.setFullName("Admin User");
		request.setRoles(Set.of("ADMIN"));

		when(appUserRepository.existsByEmail("admin@example.com")).thenReturn(true);

		assertThatThrownBy(() -> userService.createUser(request))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("already exists");
		verify(appUserRepository, never()).save(any());
	}

	@Test
	void createUserRejectsUnknownRole() {
		CreateUserRequest request = new CreateUserRequest();
		request.setEmail("admin@example.com");
		request.setPassword("password123");
		request.setFullName("Admin User");
		request.setRoles(Set.of("MANAGER"));

		assertThatThrownBy(() -> userService.createUser(request))
				.isInstanceOf(InvalidRequestException.class)
				.hasMessageContaining("Unknown role");
	}

	@Test
	void getUserByIdThrowsWhenMissing() {
		when(appUserRepository.findById(99L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> userService.getUserById(99L))
				.isInstanceOf(ResourceNotFoundException.class)
				.hasMessageContaining("99");
	}

	@Test
	void updateUserStatusPreventsDisablingLastAdmin() {
		AppUser admin = new AppUser("admin@example.com", "bcrypt-hash", "Admin", true);
		ReflectionTestUtils.setField(admin, "id", 1L);
		admin.setRoles(Set.of(adminRole));

		when(appUserRepository.findById(1L)).thenReturn(Optional.of(admin));
		when(appUserRepository.countByEnabledTrueAndRoles_Name(RoleName.ADMIN)).thenReturn(1L);

		UpdateUserStatusRequest request = new UpdateUserStatusRequest();
		request.setEnabled(false);

		assertThatThrownBy(() -> userService.updateUserStatus(1L, request))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("last enabled ADMIN");
	}

	@Test
	void getAllUsersReturnsDeterministicMappedResponses() {
		AppUser first = new AppUser("a@example.com", "hash-a", "A", true);
		ReflectionTestUtils.setField(first, "id", 1L);
		first.setRoles(Set.of(waiterRole));

		AppUser second = new AppUser("b@example.com", "hash-b", "B", false);
		ReflectionTestUtils.setField(second, "id", 2L);
		second.setRoles(Set.of(adminRole));

		when(appUserRepository.findAllByOrderByIdAsc()).thenReturn(List.of(first, second));

		List<UserResponse> users = userService.getAllUsers();

		assertThat(users).hasSize(2);
		assertThat(users.get(0).getId()).isEqualTo(1L);
		assertThat(users.get(0).getRoles()).containsExactly("WAITER");
		assertThat(users.get(1).isEnabled()).isFalse();
	}
}
