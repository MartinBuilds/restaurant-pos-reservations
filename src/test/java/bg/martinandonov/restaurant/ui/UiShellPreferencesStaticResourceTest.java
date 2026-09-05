package bg.martinandonov.restaurant.ui;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class UiShellPreferencesStaticResourceTest {

	private static final Pattern KEY_PATTERN = Pattern.compile("'([a-zA-Z0-9_.]+)'\\s*:");

	@Test
	void sharedThemeLanguageAndSidebarAssetsExist() {
		assertExists("static/shared/js/theme-boot.js");
		assertExists("static/shared/js/ui-preferences.js");
		assertExists("static/shared/js/account-shell.js");
		assertExists("static/shared/js/icons.js");
		assertExists("static/shared/js/i18n/i18n.js");
		assertExists("static/shared/js/i18n/bg.js");
		assertExists("static/shared/js/i18n/en.js");
		assertExists("static/shared/css/theme.css");
		assertExists("static/shared/css/shell.css");
	}

	@Test
	void dictionariesShareRequiredNavigationAndAccountKeys() throws Exception {
		String bg = read("static/shared/js/i18n/bg.js");
		String en = read("static/shared/js/i18n/en.js");
		var bgKeys = extractKeys(bg);
		var enKeys = extractKeys(en);

		assertThat(bgKeys).containsAll(List.of(
				"nav.dashboard", "nav.users", "nav.menu", "nav.inventory", "nav.tables",
				"nav.orders", "nav.reservations", "nav.payments", "nav.reports",
				"account.theme", "account.language", "account.logout",
				"theme.system", "theme.light", "theme.dark",
				"lang.bg", "lang.en",
				"sidebar.collapse", "sidebar.expand",
				"dashboard.welcome", "payment.simulationWarning"));
		assertThat(enKeys).containsAll(bgKeys);
		assertThat(bgKeys).containsAll(enKeys);
	}

	@Test
	void themeSupportsSystemLightDarkAndSemanticVariables() throws Exception {
		String prefs = read("static/shared/js/ui-preferences.js");
		String css = read("static/shared/css/theme.css");
		String boot = read("static/shared/js/theme-boot.js");

		assertThat(prefs).contains("restaurant.ui.theme");
		assertThat(prefs).contains("restaurant.ui.language");
		assertThat(prefs).contains("restaurant.ui.sidebar.collapsed");
		assertThat(prefs).contains("prefers-color-scheme");
		assertThat(prefs).contains("'system'");
		assertThat(prefs).contains("'light'");
		assertThat(prefs).contains("'dark'");
		assertThat(css).contains("--color-bg");
		assertThat(css).contains("--color-surface");
		assertThat(css).contains("--color-text");
		assertThat(css).contains("data-theme=\"light\"");
		assertThat(css).contains("data-theme=\"dark\"");
		assertThat(boot).contains("restaurant.ui.theme");
		assertThat(boot).doesNotContain("/api/");
		assertThat(boot).doesNotContain("password");
	}

	@Test
	void roleShellsIncludeAccountThemeLanguageAndNoCdn() throws Exception {
		for (String htmlPath : List.of(
				"static/admin/index.html",
				"static/waiter/index.html",
				"static/kitchen/index.html",
				"static/client/index.html")) {
			String html = read(htmlPath);
			assertThat(html).contains("/shared/js/theme-boot.js");
			assertThat(html).contains("/shared/css/theme.css");
			assertThat(html).contains("/shared/css/shell.css");
			assertThat(html).contains("id=\"account-mount\"");
			assertThat(html.toLowerCase()).doesNotContain("cdn.");
			assertThat(html.toLowerCase()).doesNotContain("googleapis");
			assertThat(html).doesNotContain("font-awesome");
			assertThat(html).doesNotContain("jquery");
			assertThat(html).doesNotContain("bootstrap");
			assertThat(html).doesNotContain("tailwind");
			assertThat(html).doesNotContain("react");
		}

		String admin = read("static/admin/index.html");
		assertThat(admin).contains("id=\"sidebar-collapse-btn\"");
		assertThat(admin).contains("data-i18n-aria=\"sidebar.collapse\"");

		String shellCss = read("static/shared/css/shell.css");
		assertThat(shellCss).contains("is-collapsed");
		assertThat(shellCss).contains("account-menu");

		String icons = read("static/shared/js/icons.js");
		assertThat(icons).contains("dashboard");
		assertThat(icons).contains("logout");
		assertThat(icons).contains("createElementNS");
		assertThat(icons.toLowerCase()).doesNotContain("cdn.");
	}

	@Test
	void preferencesDoNotStoreSecrets() throws Exception {
		String prefs = read("static/shared/js/ui-preferences.js");
		assertThat(prefs.toLowerCase()).doesNotContain("password");
		assertThat(prefs.toLowerCase()).doesNotContain("csrf");
		assertThat(prefs.toLowerCase()).doesNotContain("session");
		assertThat(prefs).doesNotContain("token");
	}

	private static void assertExists(String classpathLocation) {
		assertThat(new ClassPathResource(classpathLocation).exists())
				.as(classpathLocation)
				.isTrue();
	}

	private static String read(String classpathLocation) throws Exception {
		ClassPathResource resource = new ClassPathResource(classpathLocation);
		return new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
	}

	private static java.util.Set<String> extractKeys(String source) {
		Matcher matcher = KEY_PATTERN.matcher(source);
		return matcher.results().map(r -> r.group(1)).collect(Collectors.toSet());
	}
}
