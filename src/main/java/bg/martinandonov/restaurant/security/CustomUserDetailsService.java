package bg.martinandonov.restaurant.security;

import java.util.stream.Collectors;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import bg.martinandonov.restaurant.user.EmailNormalizer;
import bg.martinandonov.restaurant.user.entity.AppUser;
import bg.martinandonov.restaurant.user.repository.AppUserRepository;

@Service
public class CustomUserDetailsService implements UserDetailsService {

	private final AppUserRepository appUserRepository;

	public CustomUserDetailsService(AppUserRepository appUserRepository) {
		this.appUserRepository = appUserRepository;
	}

	@Override
	@Transactional(readOnly = true)
	public UserDetails loadUserByUsername(String username) {
		String email = EmailNormalizer.normalize(username);
		if (email == null || email.isBlank()) {
			throw new UsernameNotFoundException("User not found");
		}

		AppUser user = appUserRepository.findByEmail(email)
				.orElseThrow(() -> new UsernameNotFoundException("User not found"));

		return User.builder()
				.username(user.getEmail())
				.password(user.getPassword())
				.disabled(!user.isEnabled())
				.authorities(user.getRoles().stream()
						.map(role -> new SimpleGrantedAuthority("ROLE_" + role.getName().name()))
						.collect(Collectors.toSet()))
				.build();
	}
}
