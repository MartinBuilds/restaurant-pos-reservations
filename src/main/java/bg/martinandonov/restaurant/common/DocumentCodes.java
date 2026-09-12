package bg.martinandonov.restaurant.common;

import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Predicate;

/**
 * Short human-readable document codes for UI columns (not raw UUIDs).
 */
public final class DocumentCodes {

	private static final DateTimeFormatter DAY = DateTimeFormatter.BASIC_ISO_DATE;
	private static final int MAX_ATTEMPTS = 8;

	private DocumentCodes() {
	}

	public static String reservation(Clock clock) {
		return coded("RES", clock);
	}

	public static String order(Clock clock) {
		return coded("ORD", clock);
	}

	public static String receipt(Clock clock) {
		return coded("RCP", clock);
	}

	public static String unique(String prefix, Clock clock, Predicate<String> alreadyExists) {
		for (int i = 0; i < MAX_ATTEMPTS; i++) {
			String candidate = coded(prefix, clock);
			if (!alreadyExists.test(candidate)) {
				return candidate;
			}
		}
		return coded(prefix, clock) + ThreadLocalRandom.current().nextInt(10, 99);
	}

	private static String coded(String prefix, Clock clock) {
		String day = LocalDate.now(clock).format(DAY);
		int suffix = ThreadLocalRandom.current().nextInt(1000, 10000);
		return prefix + "-" + day + "-" + suffix;
	}
}
