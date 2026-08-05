package bg.martinandonov.restaurant.admin;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class AdminUiStaticResourceTest {

	@Test
	void faviconExists() {
		assertResourceExists("static/favicon.ico");
	}

	@Test
	void requiredAdminStaticAssetsExist() throws Exception {
		assertResourceExists("static/admin/index.html");
		assertResourceExists("static/admin/css/admin.css");
		assertResourceExists("static/admin/js/app.js");
		assertResourceExists("static/admin/js/api.js");
		assertResourceExists("static/admin/js/router.js");
		assertResourceExists("static/admin/js/ui.js");
		assertResourceExists("static/admin/js/format.js");
		for (String view : List.of(
				"dashboard", "users", "menu", "inventory", "tables", "reservations", "payments", "reports")) {
			assertResourceExists("static/admin/js/views/" + view + ".js");
		}
	}

	@Test
	void indexUsesModuleScriptAndHasNoForbiddenDependencies() throws Exception {
		String html = read("static/admin/index.html");
		assertThat(html).contains("type=\"module\"");
		assertThat(html).contains("/admin/js/app.js");
		assertThat(html).doesNotContain("onclick=");
		assertThat(html.toLowerCase()).doesNotContain("cdn.");
		assertThat(html.toLowerCase()).doesNotContain("unpkg.com");
		assertThat(html.toLowerCase()).doesNotContain("jsdelivr");
		assertThat(html.toLowerCase()).doesNotContain("googleapis");
		assertThat(html).doesNotContain("react");
		assertThat(html).doesNotContain("angular");
		assertThat(html).doesNotContain("vue");
		assertThat(html).doesNotContain("jquery");
		assertThat(html).doesNotContain("bootstrap");
		assertThat(html).doesNotContain("tailwind");
	}

	@Test
	void javascriptSourcesAvoidFrameworkAndCdnImports() throws Exception {
		for (String path : List.of(
				"static/admin/js/app.js",
				"static/admin/js/api.js",
				"static/admin/js/views/dashboard.js",
				"static/admin/js/views/reports.js")) {
			String js = read(path).toLowerCase();
			assertThat(js).doesNotContain("from 'react'");
			assertThat(js).doesNotContain("from \"react\"");
			assertThat(js).doesNotContain("cdn.");
			assertThat(js).doesNotContain("https://");
			assertThat(js).doesNotContain("onclick=");
		}
		assertThat(new ClassPathResource("static/admin/package.json").exists()).isFalse();
		assertThat(new ClassPathResource("static/admin/node_modules").exists()).isFalse();
	}

	@Test
	void cssHasNoExternalFontImports() throws Exception {
		String css = read("static/admin/css/admin.css").toLowerCase();
		assertThat(css).doesNotContain("@import url(");
		assertThat(css).doesNotContain("fonts.googleapis");
		assertThat(css).doesNotContain("fonts.gstatic");
	}

	private static void assertResourceExists(String classpathLocation) {
		assertThat(new ClassPathResource(classpathLocation).exists())
				.as(classpathLocation)
				.isTrue();
	}

	private static String read(String classpathLocation) throws Exception {
		ClassPathResource resource = new ClassPathResource(classpathLocation);
		return new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
	}
}