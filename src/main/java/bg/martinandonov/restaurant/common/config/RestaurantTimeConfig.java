package bg.martinandonov.restaurant.common.config;

import java.time.Clock;
import java.time.ZoneId;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RestaurantTimeConfig {

	@Bean
	ZoneId restaurantZoneId(
			@Value("${app.restaurant.time-zone:Europe/Sofia}") String timeZone) {
		return ZoneId.of(timeZone);
	}

	@Bean
	Clock restaurantClock(ZoneId restaurantZoneId) {
		return Clock.system(restaurantZoneId);
	}
}
