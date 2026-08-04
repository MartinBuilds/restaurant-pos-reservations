package bg.martinandonov.restaurant.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;

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
		http
				.authorizeHttpRequests(auth -> auth
						.requestMatchers(
								"/",
								"/error",
								"/favicon.ico",
								"/css/**",
								"/js/**",
								"/images/**",
								"/login",
								"/api/public/**")
						.permitAll()
						.requestMatchers("/api/admin/**").hasRole("ADMIN")
						.requestMatchers("/api/waiter/**").hasAnyRole("WAITER", "ADMIN")
						.requestMatchers("/api/kitchen/**").hasAnyRole("COOK", "ADMIN")
						.requestMatchers("/api/client/**").hasAnyRole("CLIENT", "ADMIN")
						.requestMatchers("/api/**").authenticated()
						.anyRequest().authenticated())
				.formLogin(Customizer.withDefaults())
				.logout(Customizer.withDefaults())
				.exceptionHandling(ex -> ex.defaultAuthenticationEntryPointFor(
						new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED),
						PathPatternRequestMatcher.pathPattern("/api/**")));

		return http.build();
	}
}
