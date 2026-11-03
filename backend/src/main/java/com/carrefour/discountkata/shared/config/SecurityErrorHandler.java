package com.carrefour.discountkata.shared.config;

import com.carrefour.discountkata.discount.adapter.in.web.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class SecurityErrorHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

	// Sets the WWW-Authenticate header required by RFC 6750 on 401 responses
	private final BearerTokenAuthenticationEntryPoint bearerEntryPoint = new BearerTokenAuthenticationEntryPoint();

	private final JsonMapper jsonMapper;

	@Override
	public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception)
			throws IOException {
		log.debug("Authentication failed on {} {}: {}", request.getMethod(), request.getRequestURI(),
				exception.getMessage());
		bearerEntryPoint.commence(request, response, exception);
		write(response, HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "A valid bearer token is required.");
	}

	@Override
	public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException exception)
			throws IOException {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		log.warn("Access denied: subject={} on {} {}", authentication == null ? "anonymous" : authentication.getName(),
				request.getMethod(), request.getRequestURI());
		write(response, HttpStatus.FORBIDDEN, "FORBIDDEN", "You are not allowed to perform this operation.");
	}

	private void write(HttpServletResponse response, HttpStatus status, String code, String message)
			throws IOException {
		response.setStatus(status.value());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		jsonMapper.writeValue(response.getOutputStream(), ErrorResponse.of(status.value(), code, message));
	}
}
