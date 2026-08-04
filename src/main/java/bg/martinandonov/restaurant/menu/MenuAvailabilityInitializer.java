package bg.martinandonov.restaurant.menu;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import bg.martinandonov.restaurant.menu.service.MenuAvailabilityService;

@Component
@Profile("!test")
public class MenuAvailabilityInitializer implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(MenuAvailabilityInitializer.class);

	private final MenuAvailabilityService menuAvailabilityService;

	public MenuAvailabilityInitializer(MenuAvailabilityService menuAvailabilityService) {
		this.menuAvailabilityService = menuAvailabilityService;
	}

	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		int updated = menuAvailabilityService.recalculateAllMenuItems().size();
		log.info("Recalculated availability for {} menu items", updated);
	}
}
