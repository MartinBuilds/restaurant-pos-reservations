package bg.martinandonov.restaurant.common;

import org.springframework.http.MediaType;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.util.HtmlUtils;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Product-facing login page. Replaces Spring Security's default generated form.
 * CSRF remains required on POST /login; token is embedded only for this form.
 */
@Controller
public class LoginPageController {

	@GetMapping(value = "/login", produces = MediaType.TEXT_HTML_VALUE)
	@ResponseBody
	public String loginPage(HttpServletRequest request, CsrfToken csrf) {
		boolean error = request.getParameter("error") != null;
		boolean loggedOut = request.getParameter("logout") != null;
		String csrfName = HtmlUtils.htmlEscape(csrf != null ? csrf.getParameterName() : "_csrf");
		String csrfValue = HtmlUtils.htmlEscape(csrf != null ? csrf.getToken() : "");

		String alert = "";
		if (error) {
			alert = """
					<div class="login-alert" data-i18n="login.error" role="alert">
					  Невалиден имейл или парола. Опитайте отново.
					</div>
					""";
		} else if (loggedOut) {
			alert = """
					<div class="login-alert login-alert-ok" data-i18n="login.loggedOut" role="status">
					  Излязохте успешно.
					</div>
					""";
		}

		return """
				<!DOCTYPE html>
				<html lang="bg">
				<head>
				  <meta charset="UTF-8">
				  <meta name="viewport" content="width=device-width, initial-scale=1">
				  <title>Вход — Restaurant POS</title>
				  <script src="/shared/js/theme-boot.js"></script>
				  <link rel="stylesheet" href="/shared/css/theme.css?v=pr19-1">
				  <link rel="stylesheet" href="/shared/css/shell.css?v=pr19-1">
				  <link rel="stylesheet" href="/shared/css/login.css?v=pr19-1">
				</head>
				<body>
				  <main class="login-page">
				    <section class="login-card" aria-labelledby="login-title">
				      <div class="login-toolbar">
				        <button type="button" id="login-lang-bg" data-i18n="lang.bg">Български</button>
				        <button type="button" id="login-lang-en" data-i18n="lang.en">English</button>
				      </div>
				      <p class="login-brand" data-i18n="app.brand">Restaurant POS</p>
				      <h1 id="login-title" data-i18n="login.title">Вход в системата</h1>
				      <p class="login-subtitle" data-i18n="login.subtitle">Влезте в профила си, за да продължите.</p>
				      %s
				      <form method="post" action="/login" class="login-form" autocomplete="on">
				        <div class="field">
				          <label for="username" data-i18n="login.email">Имейл</label>
				          <input id="username" name="username" type="email" required autofocus
				                 autocomplete="username" data-i18n-placeholder="login.email">
				        </div>
				        <div class="field">
				          <label for="password" data-i18n="login.password">Парола</label>
				          <input id="password" name="password" type="password" required
				                 autocomplete="current-password" data-i18n-placeholder="login.password">
				        </div>
				        <input type="hidden" name="%s" value="%s">
				        <button type="submit" class="btn btn-primary" data-i18n="login.submit">Вход</button>
				      </form>
				    </section>
				  </main>
				  <script type="module">
				    import { applyDomI18n, setLanguage, onLanguageChange, t } from '/shared/js/i18n/i18n.js?v=pr19-1';
				    import { getLanguage } from '/shared/js/ui-preferences.js';

				    function refresh() {
				      applyDomI18n(document);
				      document.title = t('login.title') + ' — Restaurant POS';
				      document.documentElement.lang = getLanguage() === 'en' ? 'en' : 'bg';
				    }

				    document.getElementById('login-lang-bg').addEventListener('click', () => setLanguage('bg'));
				    document.getElementById('login-lang-en').addEventListener('click', () => setLanguage('en'));
				    onLanguageChange(refresh);
				    refresh();
				  </script>
				</body>
				</html>
				""".formatted(alert, csrfName, csrfValue);
	}
}
