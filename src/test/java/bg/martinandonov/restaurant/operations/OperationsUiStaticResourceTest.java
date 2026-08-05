package bg.martinandonov.restaurant.operations;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class OperationsUiStaticResourceTest {

	@Test
	void requiredOperationsAssetsExist() {
		assertResourceExists("static/waiter/index.html");
		assertResourceExists("static/kitchen/index.html");
		assertResourceExists("static/operations/css/operations.css");
		assertResourceExists("static/operations/js/api.js");
		assertResourceExists("static/operations/js/csrf.js");
		assertResourceExists("static/operations/js/stomp-client.js");
		assertResourceExists("static/operations/js/connection-status.js");
		assertResourceExists("static/operations/js/dom.js");
		assertResourceExists("static/operations/js/format.js");
		assertResourceExists("static/operations/js/notifications.js");
		assertResourceExists("static/waiter/js/app.js");
		assertResourceExists("static/kitchen/js/app.js");
		assertResourceExists("static/kitchen/js/queue.js");
		assertResourceExists("static/kitchen/js/realtime.js");
		assertResourceExists("static/waiter/js/realtime.js");
		assertResourceExists("static/waiter/js/router.js");
		for (String view : List.of("tables", "orders", "order-form", "payment", "reservations", "ui-shared")) {
			assertResourceExists("static/waiter/js/views/" + view + ".js");
		}
	}

	@Test
	void indexesUseModulesWithoutForbiddenDeps() throws Exception {
		for (String path : List.of("static/waiter/index.html", "static/kitchen/index.html")) {
			String html = read(path).toLowerCase();
			assertThat(html).contains("type=\"module\"");
			assertThat(html).doesNotContain("onclick=");
			assertThat(html).doesNotContain("cdn.");
			assertThat(html).doesNotContain("unpkg.com");
			assertThat(html).doesNotContain("jsdelivr");
			assertThat(html).doesNotContain("googleapis");
			assertThat(html).doesNotContain("sockjs");
			assertThat(html).doesNotContain("stomp.umd");
			assertThat(html).doesNotContain("react");
			assertThat(html).doesNotContain("angular");
			assertThat(html).doesNotContain("vue");
			assertThat(html).doesNotContain("jquery");
			assertThat(html).doesNotContain("bootstrap");
			assertThat(html).doesNotContain("tailwind");
			assertThat(html).doesNotContain("cvv");
			assertThat(html).doesNotContain("card number");
		}
	}

	@Test
	void javascriptAvoidsFrameworkCdnSockjsAndBusinessSend() throws Exception {
		for (String path : List.of(
				"static/operations/js/stomp-client.js",
				"static/operations/js/api.js",
				"static/waiter/js/app.js",
				"static/kitchen/js/app.js",
				"static/waiter/js/realtime.js",
				"static/kitchen/js/realtime.js")) {
			String js = read(path);
			String lower = js.toLowerCase();
			assertThat(lower).doesNotContain("cdn.");
			assertThat(lower).doesNotContain("https://");
			assertThat(lower).doesNotContain("sockjs");
			assertThat(lower).doesNotContain("from 'react'");
			assertThat(lower).doesNotContain("@messagemapping");
			assertThat(js).doesNotContain("destination:/app/");
			assertThat(js).doesNotContain("buildFrame('SEND'");
			assertThat(js).doesNotContain("sendToApp");
			assertThat(js).doesNotContain("publishBusiness");
		}
		assertThat(new ClassPathResource("static/waiter/package.json").exists()).isFalse();
		assertThat(new ClassPathResource("static/kitchen/node_modules").exists()).isFalse();
		assertThat(new ClassPathResource("static/operations/package.json").exists()).isFalse();
	}

	@Test
	void cssHasNoExternalFontImports() throws Exception {
		String css = read("static/operations/css/operations.css").toLowerCase();
		assertThat(css).doesNotContain("@import url(");
		assertThat(css).doesNotContain("fonts.googleapis");
		assertThat(css).doesNotContain("fonts.gstatic");
	}

	@Test
	void stompClientHasNoBusinessSendApi() throws Exception {
		String js = read("static/operations/js/stomp-client.js");
		assertThat(js).contains("CONNECT");
		assertThat(js).contains("SUBSCRIBE");
		assertThat(js).contains("DISCONNECT");
		assertThat(js).doesNotContain("sendToApp");
		assertThat(js).doesNotContain("publishBusiness");
		assertThat(js).doesNotContain("destination:/app/");
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