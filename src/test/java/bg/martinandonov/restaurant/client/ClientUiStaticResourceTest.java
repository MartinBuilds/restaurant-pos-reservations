package bg.martinandonov.restaurant.client;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class ClientUiStaticResourceTest {

	@Test
	void requiredClientStaticAssetsExist() {
		assertResourceExists("static/client/index.html");
		assertResourceExists("static/client/css/client.css");
		assertResourceExists("static/client/js/app.js");
		assertResourceExists("static/client/js/api.js");
		assertResourceExists("static/client/js/router.js");
		assertResourceExists("static/client/js/ui.js");
		assertResourceExists("static/client/js/dom.js");
		assertResourceExists("static/client/js/format.js");
		for (String view : List.of(
				"availability", "reservations", "reservation-details", "reservation-form")) {
			assertResourceExists("static/client/js/views/" + view + ".js");
		}
	}

	@Test
	void indexUsesModuleScriptAndHasNoForbiddenDependencies() throws Exception {
		String html = read("static/client/index.html");
		assertThat(html).contains("type=\"module\"");
		assertThat(html).contains("/client/js/app.js");
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
		assertThat(html.toLowerCase()).doesNotContain("type=\"password\"");
		assertThat(html.toLowerCase()).doesNotContain("card number");
		assertThat(html.toLowerCase()).doesNotContain("cvv");
	}

	@Test
	void javascriptSourcesAvoidFrameworkCdnWebsocketAndPayments() throws Exception {
		List<String> paths = List.of(
				"static/client/js/app.js",
				"static/client/js/api.js",
				"static/client/js/router.js",
				"static/client/js/views/availability.js",
				"static/client/js/views/reservations.js",
				"static/client/js/views/reservation-details.js",
				"static/client/js/views/reservation-form.js");
		for (String path : paths) {
			String js = read(path).toLowerCase();
			assertThat(js).doesNotContain("from 'react'");
			assertThat(js).doesNotContain("from \"react\"");
			assertThat(js).doesNotContain("cdn.");
			assertThat(js).doesNotContain("https://");
			assertThat(js).doesNotContain("onclick=");
			assertThat(js).doesNotContain("websocket");
			assertThat(js).doesNotContain("stomp");
			assertThat(js).doesNotContain("sockjs");
			assertThat(js).doesNotContain("cardnumber");
			assertThat(js).doesNotContain("cvv");
			assertThat(js).doesNotContain("type=\"password\"");
		}
		assertThat(new ClassPathResource("static/client/package.json").exists()).isFalse();
		assertThat(new ClassPathResource("static/client/node_modules").exists()).isFalse();
	}

	@Test
	void cssHasNoExternalFontImports() throws Exception {
		String css = read("static/client/css/client.css").toLowerCase();
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