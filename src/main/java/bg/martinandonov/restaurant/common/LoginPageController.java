package bg.martinandonov.restaurant.common;

import org.springframework.http.MediaType;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.util.HtmlUtils;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Product-facing login / registration page.
 * CSRF remains required on POST /login and POST /api/public/register.
 */
@Controller
public class LoginPageController {

	@GetMapping(value = "/login", produces = MediaType.TEXT_HTML_VALUE)
	@ResponseBody
	public String loginPage(HttpServletRequest request, CsrfToken csrf) {
		boolean error = request.getParameter("error") != null;
		boolean loggedOut = request.getParameter("logout") != null;
		boolean registered = request.getParameter("registered") != null;
		String csrfName = HtmlUtils.htmlEscape(csrf != null ? csrf.getParameterName() : "_csrf");
		String csrfValue = HtmlUtils.htmlEscape(csrf != null ? csrf.getToken() : "");
		String csrfHeader = HtmlUtils.htmlEscape(csrf != null ? csrf.getHeaderName() : "X-CSRF-TOKEN");

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
		} else if (registered) {
			alert = """
					<div class="login-alert login-alert-ok" data-i18n="register.success" role="status">
					  Акаунтът е създаден. Влезте с имейл и парола.
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
				  <link rel="stylesheet" href="/shared/css/theme.css?v=fix-login-3">
				  <link rel="stylesheet" href="/shared/css/login.css?v=fix-login-3">
				</head>
				<body>
				  <main class="login-page">
				    <aside class="login-stage" aria-hidden="false">
				      <div class="login-stage-inner">
				        <p class="login-brand" data-i18n="app.brand">Restaurant POS</p>
				        <p class="login-stage-line" data-i18n="login.stageLine">Маси, поръчки и резервации — на едно място.</p>
				        <div class="login-stage-mark" aria-hidden="true"></div>
				      </div>
				    </aside>

				    <section class="login-panel-wrap" aria-labelledby="login-title">
				      <div class="login-panel-inner">
				        <div class="login-toolbar" role="group" aria-label="Language">
				          <button type="button" id="login-lang-bg" data-i18n="lang.bg">Български</button>
				          <button type="button" id="login-lang-en" data-i18n="lang.en">English</button>
				        </div>

				        <h1 id="login-title" data-i18n="login.title">Вход в системата</h1>
				        <p class="login-subtitle" id="login-subtitle" data-i18n="login.subtitle">Влезте в профила си, за да продължите.</p>

				        <div class="login-tabs" role="tablist" aria-label="Auth mode">
				          <button type="button" class="login-tab is-active" id="tab-login" role="tab" aria-selected="true" data-i18n="login.tab">Вход</button>
				          <button type="button" class="login-tab" id="tab-register" role="tab" aria-selected="false" data-i18n="register.tab">Регистрация</button>
				        </div>

				        <div id="auth-alert">%s</div>

				        <div class="login-panel" id="panel-login" role="tabpanel">
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
				        </div>

				        <div class="login-panel" id="panel-register" role="tabpanel" hidden>
				          <form class="register-form" autocomplete="on" novalidate>
				            <div class="field">
				              <label for="reg-name" data-i18n="register.fullName">Име</label>
				              <input id="reg-name" name="fullName" type="text" required
				                     autocomplete="name" data-i18n-placeholder="register.fullName">
				            </div>
				            <div class="field">
				              <label for="reg-email" data-i18n="login.email">Имейл</label>
				              <input id="reg-email" name="email" type="email" required
				                     autocomplete="email" data-i18n-placeholder="login.email">
				            </div>
				            <div class="field">
				              <label for="reg-password" data-i18n="login.password">Парола</label>
				              <input id="reg-password" name="password" type="password" required minlength="8"
				                     autocomplete="new-password" data-i18n-placeholder="register.passwordHint">
				            </div>
				            <p class="login-field-error" id="register-error" hidden></p>
				            <button type="submit" class="btn btn-primary" id="register-submit" data-i18n="register.submit">Създай акаунт</button>
				            <p class="login-hint" data-i18n="register.clientOnly">Регистрацията създава клиентски профил за онлайн резервации.</p>
				          </form>
				        </div>
				      </div>
				    </section>
				  </main>
				  <script type="module">
				    import { applyDomI18n, setLanguage, onLanguageChange, t } from '/shared/js/i18n/i18n.js?v=fix-login-3';
				    import { getLanguage } from '/shared/js/ui-preferences.js';
				    import { clearJustSignedIn, markJustSignedIn } from '/shared/js/session-flash.js?v=fix-toasts-3';

				    const CSRF_HEADER = %s;
				    const CSRF_TOKEN = %s;

				    const tabLogin = document.getElementById('tab-login');
				    const tabRegister = document.getElementById('tab-register');
				    const panelLogin = document.getElementById('panel-login');
				    const panelRegister = document.getElementById('panel-register');
				    const title = document.getElementById('login-title');
				    const subtitle = document.getElementById('login-subtitle');
				    const alertHost = document.getElementById('auth-alert');
				    const registerError = document.getElementById('register-error');
				    const registerSubmit = document.getElementById('register-submit');
				    const langBg = document.getElementById('login-lang-bg');
				    const langEn = document.getElementById('login-lang-en');

				    let mode = 'login';

				    function refresh() {
				      applyDomI18n(document);
				      document.documentElement.lang = getLanguage() === 'en' ? 'en' : 'bg';
				      langBg.classList.toggle('is-active', getLanguage() === 'bg');
				      langEn.classList.toggle('is-active', getLanguage() === 'en');
				      if (mode === 'register') {
				        title.setAttribute('data-i18n', 'register.title');
				        subtitle.setAttribute('data-i18n', 'register.subtitle');
				        document.title = t('register.title') + ' — Restaurant POS';
				      } else {
				        title.setAttribute('data-i18n', 'login.title');
				        subtitle.setAttribute('data-i18n', 'login.subtitle');
				        document.title = t('login.title') + ' — Restaurant POS';
				      }
				      title.textContent = t(title.getAttribute('data-i18n'));
				      subtitle.textContent = t(subtitle.getAttribute('data-i18n'));
				    }

				    function setMode(next) {
				      mode = next === 'register' ? 'register' : 'login';
				      const isRegister = mode === 'register';
				      tabLogin.classList.toggle('is-active', !isRegister);
				      tabRegister.classList.toggle('is-active', isRegister);
				      tabLogin.setAttribute('aria-selected', String(!isRegister));
				      tabRegister.setAttribute('aria-selected', String(isRegister));
				      panelLogin.hidden = isRegister;
				      panelRegister.hidden = !isRegister;
				      if (alertHost && isRegister) alertHost.innerHTML = '';
				      registerError.hidden = true;
				      registerError.textContent = '';
				      refresh();
				      const focusId = isRegister ? 'reg-name' : 'username';
				      document.getElementById(focusId)?.focus();
				    }

				    const params = new URLSearchParams(window.location.search);
				    if (params.has('error') || params.has('logout') || params.has('registered')) {
				      clearJustSignedIn();
				    }

				    const loginForm = document.querySelector('.login-form');
				    if (loginForm) {
				      loginForm.addEventListener('submit', () => markJustSignedIn());
				    }

				    tabLogin.addEventListener('click', () => setMode('login'));
				    tabRegister.addEventListener('click', () => setMode('register'));
				    langBg.addEventListener('click', () => setLanguage('bg'));
				    langEn.addEventListener('click', () => setLanguage('en'));
				    onLanguageChange(refresh);

				    document.querySelector('.register-form').addEventListener('submit', async (event) => {
				      event.preventDefault();
				      registerError.hidden = true;
				      registerError.textContent = '';
				      const fullName = document.getElementById('reg-name').value.trim();
				      const email = document.getElementById('reg-email').value.trim();
				      const password = document.getElementById('reg-password').value;
				      if (!fullName || !email || password.length < 8) {
				        registerError.textContent = t('register.validation');
				        registerError.hidden = false;
				        return;
				      }
				      registerSubmit.disabled = true;
				      try {
				        const response = await fetch('/api/public/register', {
				          method: 'POST',
				          headers: {
				            'Content-Type': 'application/json',
				            [CSRF_HEADER]: CSRF_TOKEN
				          },
				          body: JSON.stringify({ fullName, email, password })
				        });
				        if (!response.ok) {
				          let message = t('register.failed');
				          try {
				            const body = await response.json();
				            if (body && body.message) message = body.message;
				          } catch (_) { /* ignore */ }
				          registerError.textContent = message;
				          registerError.hidden = false;
				          return;
				        }
				        window.location.assign('/login?registered');
				      } catch (_) {
				        registerError.textContent = t('register.failed');
				        registerError.hidden = false;
				      } finally {
				        registerSubmit.disabled = false;
				      }
				    });

				    refresh();
				  </script>
				</body>
				</html>
				""".formatted(
				alert,
				csrfName,
				csrfValue,
				toJsString(csrfHeader),
				toJsString(csrfValue));
	}

	private static String toJsString(String value) {
		String escaped = value == null ? "" : value
				.replace("\\", "\\\\")
				.replace("\"", "\\\"")
				.replace("\r", "")
				.replace("\n", "\\n");
		return "\"" + escaped + "\"";
	}
}
