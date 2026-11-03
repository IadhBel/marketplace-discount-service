package com.carrefour.discountkata.shared.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * The only place where authorization rules are declared (RG-17). Roles come
 * from the "roles" claim of the JWT, mapped to ROLE_* authorities by the
 * resource server properties.
 */
@Configuration
public class SecurityConfiguration {

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http, SecurityErrorHandler errorHandler)
			throws Exception {
		http.csrf(AbstractHttpConfigurer::disable)
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(authorize -> authorize
						.requestMatchers("/actuator/health/**", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html")
						.permitAll().requestMatchers(HttpMethod.POST, "/api/discounts/apply").hasRole("CUSTOMER")
						.anyRequest().authenticated())
				.oauth2ResourceServer(resourceServer -> resourceServer.jwt(Customizer.withDefaults())
						.authenticationEntryPoint(errorHandler).accessDeniedHandler(errorHandler))
				.exceptionHandling(exceptions -> exceptions.authenticationEntryPoint(errorHandler)
						.accessDeniedHandler(errorHandler));
		return http.build();
	}
}
