package bg.martinandonov.restaurant.user.service;

import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import bg.martinandonov.restaurant.common.exception.BusinessRuleException;
import bg.martinandonov.restaurant.common.exception.InvalidRequestException;
import bg.martinandonov.restaurant.common.exception.ResourceNotFoundException;
import bg.martinandonov.restaurant.user.EmailNormalizer;
import bg.martinandonov.restaurant.user.dto.CreateUserRequest;
import bg.martinandonov.restaurant.user.dto.UpdateUserRolesRequest;
import bg.martinandonov.restaurant.user.dto.UpdateUserStatusRequest;
import bg.martinandonov.restaurant.user.dto.UserResponse;
import bg.martinandonov.restaurant.user.entity.AppUser;
import bg.martinandonov.restaurant.user.entity.Role;
import bg.martinandonov.restaurant.user.entity.RoleName;
import bg.martinandonov.restaurant.user.repository.AppUserRepository;
import bg.martinandonov.restaurant.user.repository.RoleRepository;

@Service
@Transactional
public class UserService {

	private static final int MIN_PASSWORD_LENGTH = 8;

	private final AppUserRepository appUserRepository;
	private final RoleRepository roleRepository;
	private final PasswordEncoder passwordEncoder;

	public UserService(
			AppUserRepository appUserRepository,
			RoleRepository roleRepository,
			PasswordEncoder passwordEncoder) {
		this.appUserRepository = appUserRepository;
		this.roleRepository = roleRepository;
		this.passwordEncoder = passwordEncoder;
	}

	public UserResponse createUser(CreateUserRequest request) {
		Objects.requireNonNull(request, "request must not be null");

		String email = requireNormalizedEmail(request.getEmail());
		String fullName = requireFullName(request.getFullName());
		String rawPassword = requirePassword(request.getPassword());
		Set<RoleName> roleNames = resolveRoleNames(request.getRoles());

		if (appUserRepository.existsByEmail(email)) {
			throw new BusinessRuleException("A user with this email already exists");
		}

		Set<Role> roles = loadRoles(roleNames);
		AppUser user = new AppUser(email, passwordEncoder.encode(rawPassword), fullName, true);
		user.setRoles(roles);

		AppUser saved = appUserRepository.save(user);
		return toResponse(saved);
	}

	@Transactional(readOnly = true)
	public List<UserResponse> getAllUsers() {
		return appUserRepository.findAllByOrderByIdAsc().stream()
				.map(this::toResponse)
				.toList();
	}

	@Transactional(readOnly = true)
	public UserResponse getUserById(Long id) {
		return toResponse(findUser(id));
	}

	public UserResponse updateUserRoles(Long id, UpdateUserRolesRequest request) {
		Objects.requireNonNull(request, "request must not be null");
		AppUser user = findUser(id);
		Set<RoleName> roleNames = resolveRoleNames(request.getRoles());
		user.setRoles(loadRoles(roleNames));
		return toResponse(user);
	}

	public UserResponse updateUserStatus(Long id, UpdateUserStatusRequest request) {
		Objects.requireNonNull(request, "request must not be null");
		if (request.getEnabled() == null) {
			throw new InvalidRequestException("enabled must be provided");
		}

		AppUser user = findUser(id);
		boolean enabling = request.getEnabled();
		if (!enabling && user.isEnabled() && isLastEnabledAdmin(user)) {
			throw new BusinessRuleException("Cannot disable the last enabled ADMIN user");
		}

		user.setEnabled(enabling);
		return toResponse(user);
	}

	private AppUser findUser(Long id) {
		if (id == null) {
			throw new InvalidRequestException("User id must be provided");
		}
		return appUserRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
	}

	private boolean isLastEnabledAdmin(AppUser user) {
		boolean isAdmin = user.getRoles().stream()
				.anyMatch(role -> role.getName() == RoleName.ADMIN);
		if (!isAdmin) {
			return false;
		}
		return appUserRepository.countByEnabledTrueAndRoles_Name(RoleName.ADMIN) <= 1;
	}

	private Set<Role> loadRoles(Set<RoleName> roleNames) {
		Set<Role> roles = new LinkedHashSet<>();
		for (RoleName roleName : roleNames) {
			Role role = roleRepository.findByName(roleName)
					.orElseThrow(() -> new InvalidRequestException("Unknown role: " + roleName));
			roles.add(role);
		}
		return roles;
	}

	private Set<RoleName> resolveRoleNames(Set<String> rawRoles) {
		if (rawRoles == null || rawRoles.isEmpty()) {
			throw new InvalidRequestException("At least one role is required");
		}

		Set<RoleName> roleNames = EnumSet.noneOf(RoleName.class);
		for (String rawRole : rawRoles) {
			if (rawRole == null || rawRole.isBlank()) {
				throw new InvalidRequestException("Role name must not be blank");
			}
			try {
				roleNames.add(RoleName.valueOf(rawRole.trim().toUpperCase(Locale.ROOT)));
			}
			catch (IllegalArgumentException ex) {
				throw new InvalidRequestException("Unknown role: " + rawRole.trim());
			}
		}
		return roleNames;
	}

	private String requireNormalizedEmail(String email) {
		String normalized = EmailNormalizer.normalize(email);
		if (normalized == null || normalized.isBlank()) {
			throw new InvalidRequestException("Email must not be blank");
		}
		if (!normalized.contains("@")) {
			throw new InvalidRequestException("Email must be a valid address");
		}
		return normalized;
	}

	private String requireFullName(String fullName) {
		if (fullName == null || fullName.isBlank()) {
			throw new InvalidRequestException("Full name must not be blank");
		}
		return fullName.trim();
	}

	private String requirePassword(String password) {
		if (password == null || password.length() < MIN_PASSWORD_LENGTH) {
			throw new InvalidRequestException("Password must be at least " + MIN_PASSWORD_LENGTH + " characters");
		}
		return password;
	}

	private UserResponse toResponse(AppUser user) {
		Set<String> roles = user.getRoles().stream()
				.map(role -> role.getName().name())
				.sorted()
				.collect(Collectors.toCollection(LinkedHashSet::new));
		return new UserResponse(user.getId(), user.getEmail(), user.getFullName(), user.isEnabled(), roles);
	}
}