package bg.martinandonov.restaurant.account.service;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import bg.martinandonov.restaurant.account.dto.CurrentAccountResponse;
import bg.martinandonov.restaurant.common.exception.ResourceNotFoundException;
import bg.martinandonov.restaurant.user.entity.AppUser;
import bg.martinandonov.restaurant.user.entity.Role;
import bg.martinandonov.restaurant.user.repository.AppUserRepository;

@Service
public class AccountService {

	private final AppUserRepository appUserRepository;

	public AccountService(AppUserRepository appUserRepository) {
		this.appUserRepository = appUserRepository;
	}

	@Transactional(readOnly = true)
	public CurrentAccountResponse getCurrentAccount(Authentication authentication) {
		Objects.requireNonNull(authentication, "authentication must not be null");
		String email = authentication.getName();
		if (email == null || email.isBlank() || "anonymousUser".equals(email)) {
			throw new ResourceNotFoundException("Authenticated user not found");
		}

		AppUser user = appUserRepository.findByEmail(email)
				.orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found"));

		Set<String> roles = user.getRoles().stream()
				.map(Role::getName)
				.map(Enum::name)
				.collect(Collectors.toCollection(LinkedHashSet::new));

		return new CurrentAccountResponse(user.getId(), user.getFullName(), user.getEmail(), roles);
	}
}
