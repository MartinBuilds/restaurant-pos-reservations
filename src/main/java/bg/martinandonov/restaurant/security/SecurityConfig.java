package bg.martinandonov.restaurant.security;

import java.io.IOException;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.RedirectStrategy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

	@Bean
	PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		RoleBasedAuthenticationSuccessHandler successHandler = new RoleBasedAuthenticationSuccessHandler();
		successHandler.setRedirectStrategy(seeOtherRedirectStrategy());

		http
				.authorizeHttpRequests(auth -> auth
						.dispatcherTypeMatchers(DispatcherType.FORWARD, DispatcherType.ERROR).permitAll()
						.requestMatchers(
								"/",
								"/error",
								"/favicon.ico",
								"/default-ui.css",
								"/css/**",
								"/js/**",
								"/images/**",
								"/login",
								"/api/public/**")
						.permitAll()
						.requestMatchers("/admin", "/admin/", "/admin/**").hasRole("ADMIN")
						.requestMatchers("/waiter", "/waiter/", "/waiter/**").hasAnyRole("WAITER", "ADMIN")
						.requestMatchers("/kitchen", "/kitchen/", "/kitchen/**").hasAnyRole("COOK", "ADMIN")
						.requestMatchers("/operations", "/operations/", "/operations/**")
						.hasAnyRole("WAITER", "COOK", "ADMIN")
						.requestMatchers("/client", "/client/", "/client/**").hasRole("CLIENT")
						.requestMatchers("/api/admin/**").hasRole("ADMIN")
						.requestMatchers("/api/waiter/**").hasAnyRole("WAITER", "ADMIN")
						.requestMatchers("/api/kitchen/**").hasAnyRole("COOK", "ADMIN")
						.requestMatchers("/api/client/**").hasAnyRole("CLIENT", "ADMIN")
						.requestMatchers("/api/**").authenticated()
						.requestMatchers("/ws", "/ws/**").authenticated()
						.anyRequest().authenticated())
				.formLogin(form -> form.successHandler(successHandler))
				.logout(Customizer.withDefaults())
				.exceptionHandling(ex -> ex
						.defaultAuthenticationEntryPointFor(
								new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED),
								PathPatternRequestMatcher.pathPattern("/api/**"))
						.defaultAuthenticationEntryPointFor(
								new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED),
								PathPatternRequestMatcher.pathPattern("/ws/**")));

		return http.build();
	}

	/**
	 * Post/Redirect/Get after form login: 303 forces the browser to follow with GET,
	 * avoiding CSRF 403 when a client would otherwise re-POST to the success URL.
	 */
	private static RedirectStrategy seeOtherRedirectStrategy() {
		return new RedirectStrategy() {
			@Override
			public void sendRedirect(
					HttpServletRequest request,
					HttpServletResponse response,
					String url) throws IOException {
				String target = url;
				if (target.startsWith("/") && !target.startsWith("//")) {
					target = request.getContextPath() + target;
				}
				response.setStatus(HttpServletResponse.SC_SEE_OTHER);
				response.setHeader("Location", response.encodeRedirectURL(target));
			}
		};
	}
}