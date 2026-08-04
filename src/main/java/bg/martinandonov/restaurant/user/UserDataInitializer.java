package bg.martinandonov.restaurant.user;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import bg.martinandonov.restaurant.user.entity.AppUser;
import bg.martinandonov.restaurant.user.entity.Role;
import bg.martinandonov.restaurant.user.entity.RoleName;
import bg.martinandonov.restaurant.user.repository.AppUserRepository;
import bg.martinandonov.restaurant.user.repository.RoleRepository;

@Component
@Profile("!test")
public class UserDataInitializer implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(UserDataInitializer.class);

	private final RoleRepository roleRepository;
	private final AppUserRepository appUserRepository;
	private final PasswordEncoder passwordEncoder;
	private final Environment environment;

	public UserDataInitializer(
			RoleRepository roleRepository,
			AppUserRepository appUserRepository,
			PasswordEncoder passwordEncoder,
			Environment environment) {
		this.roleRepository = roleRepository;
		this.appUserRepository = appUserRepository;
		this.passwordEncoder = passwordEncoder;
		this.environment = environment;
	}

	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		seedRoles();
		seedInitialAdminIfConfigured();
	}

	private void seedRoles() {
		for (RoleName roleName : RoleName.values()) {
			roleRepository.findByName(roleName).orElseGet(() -> roleRepository.save(new Role(roleName)));
		}
		log.info("Ensured application roles exist");
	}

	private void seedInitialAdminIfConfigured() {
		String email = EmailNormalizer.normalize(environment.getProperty("INITIAL_ADMIN_EMAIL"));
		String password = environment.getProperty("INITIAL_ADMIN_PASSWORD");
		String fullName = environment.getProperty("INITIAL_ADMIN_FULL_NAME");

		if (isBlank(email) || isBlank(password) || isBlank(fullName)) {
			log.info("Initial admin not created: INITIAL_ADMIN_* environment variables are incomplete or missing");
			return;
		}

		if (appUserRepository.existsByEmail(email)) {
			log.info("Initial admin not created: user with configured email already exists");
			return;
		}

		Role adminRole = roleRepository.findByName(RoleName.ADMIN)
				.orElseThrow(() -> new IllegalStateException("ADMIN role is missing"));

		AppUser admin = new AppUser(email, passwordEncoder.encode(password), fullName.trim(), true);
		admin.getRoles().add(adminRole);
		appUserRepository.save(admin);
		log.info("Initial ADMIN user created for email {}", email);
	}

	private boolean isBlank(String value) {
		return value == null || value.isBlank();
	}
}
