package bg.martinandonov.restaurant.account.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import bg.martinandonov.restaurant.account.dto.CurrentAccountResponse;
import bg.martinandonov.restaurant.common.exception.ResourceNotFoundException;
import bg.martinandonov.restaurant.user.entity.AppUser;
import bg.martinandonov.restaurant.user.entity.Role;
import bg.martinandonov.restaurant.user.entity.RoleName;
import bg.martinandonov.restaurant.user.repository.AppUserRepository;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

	@Mock
	private AppUserRepository appUserRepository;

	private AccountService accountService;

	@BeforeEach
	void setUp() {
		accountService = new AccountService(appUserRepository);
	}

	@Test
	void returnsCurrentUserFromAuthenticationName() {
		AppUser user = new AppUser("admin@example.com", "{bcrypt}hash", "Martin Petrov", true);
		Role admin = mock(Role.class);
		when(admin.getName()).thenReturn(RoleName.ADMIN);
		user.setRoles(Set.of(admin));
		when(appUserRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(user));

		Authentication authentication = new UsernamePasswordAuthenticationToken(
				"admin@example.com", "n/a", Set.of());

		CurrentAccountResponse response = accountService.getCurrentAccount(authentication);

		assertThat(response.getEmail()).isEqualTo("admin@example.com");
		assertThat(response.getName()).isEqualTo("Martin Petrov");
		assertThat(response.getRoles()).containsExactly("ADMIN");
		assertThat(response.getId()).isNull();
		verify(appUserRepository).findByEmail("admin@example.com");
	}

	@Test
	void refusesAnonymousAuthentication() {
		Authentication authentication = new UsernamePasswordAuthenticationToken(
				"anonymousUser", "n/a");

		assertThatThrownBy(() -> accountService.getCurrentAccount(authentication))
				.isInstanceOf(ResourceNotFoundException.class);
	}

	@Test
	void refusesMissingUser() {
		when(appUserRepository.findByEmail("gone@example.com")).thenReturn(Optional.empty());
		Authentication authentication = new UsernamePasswordAuthenticationToken(
				"gone@example.com", "n/a", Set.of());

		assertThatThrownBy(() -> accountService.getCurrentAccount(authentication))
				.isInstanceOf(ResourceNotFoundException.class);
	}
}
