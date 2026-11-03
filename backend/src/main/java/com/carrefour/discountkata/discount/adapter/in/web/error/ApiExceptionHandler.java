package com.carrefour.discountkata.discount.adapter.in.web.error;

import com.carrefour.discountkata.discount.adapter.in.web.dto.ErrorResponse;
import com.carrefour.discountkata.discount.domain.exception.DiscountCodeExpiredException;
import com.carrefour.discountkata.discount.domain.exception.DiscountCodeInactiveException;
import com.carrefour.discountkata.discount.domain.exception.DiscountCodeNotFoundException;
import com.carrefour.discountkata.discount.domain.exception.DiscountCodeNotYetValidException;
import com.carrefour.discountkata.discount.domain.exception.DiscountException;
import com.carrefour.discountkata.discount.domain.exception.InvalidCartException;
import com.carrefour.discountkata.discount.domain.exception.ProductNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.List;

/**
 * Translates every error into the error format of the API. Spring MVC errors
 * (validation, unreadable body, wrong method or media type, unknown path) go
 * through {@link ResponseEntityExceptionHandler}, which keeps their status.
 */
@Slf4j
@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

	private static final String VALIDATION_ERROR = "VALIDATION_ERROR";

	@ExceptionHandler(DiscountException.class)
	public ResponseEntity<ErrorResponse> handleBusinessError(DiscountException exception) {
		var error = switch (exception) {
			case DiscountCodeNotFoundException e -> new BusinessError(HttpStatus.NOT_FOUND, "DISCOUNT_CODE_NOT_FOUND");
			case DiscountCodeInactiveException e ->
				new BusinessError(HttpStatus.UNPROCESSABLE_CONTENT, "DISCOUNT_CODE_INACTIVE");
			case DiscountCodeNotYetValidException e ->
				new BusinessError(HttpStatus.UNPROCESSABLE_CONTENT, "DISCOUNT_CODE_NOT_YET_VALID");
			case DiscountCodeExpiredException e ->
				new BusinessError(HttpStatus.UNPROCESSABLE_CONTENT, "DISCOUNT_CODE_EXPIRED");
			case ProductNotFoundException e -> new BusinessError(HttpStatus.NOT_FOUND, "PRODUCT_NOT_FOUND");
			case InvalidCartException e -> new BusinessError(HttpStatus.BAD_REQUEST, VALIDATION_ERROR);
		};

		if (exception instanceof InvalidCartException) {
			log.debug("Invalid cart: {}", exception.getMessage());
		} else {
			log.info("Discount request rejected: reason={}, detail={}", error.code(), exception.getMessage());
		}
		return ResponseEntity.status(error.status())
				.body(ErrorResponse.of(error.status().value(), error.code(), exception.getMessage()));
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorResponse> handleUnexpectedError(Exception exception) {
		log.error("Unexpected error", exception);
		return ResponseEntity.internalServerError()
				.body(ErrorResponse.of(500, "INTERNAL_ERROR", "An unexpected error occurred."));
	}

	@Override
	protected ResponseEntity<Object> handleExceptionInternal(Exception exception, Object body, HttpHeaders headers,
			HttpStatusCode statusCode, WebRequest request) {
		var error = switch (exception) {
			case MethodArgumentNotValidException e ->
				new ErrorResponse(statusCode.value(), VALIDATION_ERROR, "The request is invalid.", fieldErrors(e));
			case HttpMessageNotReadableException e ->
				ErrorResponse.of(statusCode.value(), VALIDATION_ERROR, "The request body is not valid JSON.");
			default -> ErrorResponse.of(statusCode.value(), statusName(statusCode), detail(body, exception));
		};
		log.debug("Request rejected by Spring MVC: status={}, detail={}", statusCode.value(), exception.getMessage());
		return super.handleExceptionInternal(exception, error, headers, statusCode, request);
	}

	private static List<String> fieldErrors(MethodArgumentNotValidException exception) {
		return exception.getBindingResult().getFieldErrors().stream()
				.map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage()).toList();
	}

	private static String statusName(HttpStatusCode statusCode) {
		var status = HttpStatus.resolve(statusCode.value());
		return status == null ? "HTTP_" + statusCode.value() : status.name();
	}

	private static String detail(Object body, Exception exception) {
		return body instanceof ProblemDetail problemDetail && problemDetail.getDetail() != null
				? problemDetail.getDetail()
				: exception.getMessage();
	}

	private record BusinessError(HttpStatus status, String code) {
	}
}
