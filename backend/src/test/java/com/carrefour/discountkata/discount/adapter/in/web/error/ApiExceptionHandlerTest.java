package com.carrefour.discountkata.discount.adapter.in.web.error;

import com.carrefour.discountkata.discount.adapter.in.web.dto.ErrorResponse;
import com.carrefour.discountkata.discount.domain.exception.DiscountCodeExpiredException;
import com.carrefour.discountkata.discount.domain.exception.DiscountCodeInactiveException;
import com.carrefour.discountkata.discount.domain.exception.DiscountCodeNotFoundException;
import com.carrefour.discountkata.discount.domain.exception.DiscountCodeNotYetValidException;
import com.carrefour.discountkata.discount.domain.exception.DiscountException;
import com.carrefour.discountkata.discount.domain.exception.InvalidCartException;
import com.carrefour.discountkata.discount.domain.exception.ProductNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.context.request.ServletWebRequest;

import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.SoftAssertions.assertSoftly;

class ApiExceptionHandlerTest {

	private final ApiExceptionHandler handler = new ApiExceptionHandler();

	private final ServletWebRequest request = new ServletWebRequest(new MockHttpServletRequest());

	static Stream<Arguments> businessErrors() {
		return Stream.of(Arguments.of(new DiscountCodeNotFoundException("UNKNOWN"), 404, "DISCOUNT_CODE_NOT_FOUND"),
				Arguments.of(new DiscountCodeInactiveException("DISABLED15"), 422, "DISCOUNT_CODE_INACTIVE"),
				Arguments.of(new DiscountCodeNotYetValidException("FUTURE25"), 422, "DISCOUNT_CODE_NOT_YET_VALID"),
				Arguments.of(new DiscountCodeExpiredException("EXPIRED20"), 422, "DISCOUNT_CODE_EXPIRED"),
				Arguments.of(new ProductNotFoundException(List.of("P-999")), 404, "PRODUCT_NOT_FOUND"), Arguments.of(
						new InvalidCartException("The cart must contain at least one item."), 400, "VALIDATION_ERROR"));
	}

	@ParameterizedTest
	@MethodSource("businessErrors")
	void shouldMapBusinessError_toItsStatusAndCode(DiscountException exception, int expectedStatus,
			String expectedCode) {
		// given
		var expectedMessage = exception.getMessage();

		// when
		var response = handler.handleBusinessError(exception);

		// then
		assertSoftly(softly -> {
			softly.assertThat(response.getStatusCode().value()).isEqualTo(expectedStatus);
			softly.assertThat(response.getBody().status()).isEqualTo(expectedStatus);
			softly.assertThat(response.getBody().code()).isEqualTo(expectedCode);
			softly.assertThat(response.getBody().message()).isEqualTo(expectedMessage);
		});
	}

	@Test
	void shouldListFieldErrors_whenRequestIsInvalid() throws Exception {
		// given
		var bindingResult = new BeanPropertyBindingResult(new Object(), "applyDiscountRequest");
		bindingResult.addError(
				new FieldError("applyDiscountRequest", "items[0].quantity", "must be greater than or equal to 1"));
		var parameter = new MethodParameter(Object.class.getMethod("equals", Object.class), 0);
		var exception = new MethodArgumentNotValidException(parameter, bindingResult);

		// when
		var response = handler.handleException(exception, request);

		// then
		var body = (ErrorResponse) response.getBody();
		assertSoftly(softly -> {
			softly.assertThat(response.getStatusCode().value()).isEqualTo(400);
			softly.assertThat(body.code()).isEqualTo("VALIDATION_ERROR");
			softly.assertThat(body.details()).containsExactly("items[0].quantity: must be greater than or equal to 1");
		});
	}

	@Test
	void shouldReturnValidationError_whenBodyIsNotReadable() throws Exception {
		// given
		var exception = new HttpMessageNotReadableException("Unexpected end of input",
				new MockHttpInputMessage(new byte[0]));

		// when
		var response = handler.handleException(exception, request);

		// then
		var body = (ErrorResponse) response.getBody();
		assertSoftly(softly -> {
			softly.assertThat(response.getStatusCode().value()).isEqualTo(400);
			softly.assertThat(body.code()).isEqualTo("VALIDATION_ERROR");
		});
	}

	@Test
	void shouldKeepSpringStatusAndHeaders_whenErrorIsRaisedBySpringMvc() throws Exception {
		// given
		var exception = new HttpRequestMethodNotSupportedException("DELETE", Set.of("POST"));

		// when
		var response = handler.handleException(exception, request);

		// then
		var body = (ErrorResponse) response.getBody();
		assertSoftly(softly -> {
			softly.assertThat(response.getStatusCode().value()).isEqualTo(405);
			softly.assertThat(response.getHeaders().getFirst(HttpHeaders.ALLOW)).isEqualTo("POST");
			softly.assertThat(body.code()).isEqualTo("METHOD_NOT_ALLOWED");
			softly.assertThat(body.message()).isNotBlank();
		});
	}

	@Test
	void shouldHideTechnicalDetails_whenErrorIsUnexpected() {
		// given
		var exception = new IllegalStateException("Connection pool exhausted");

		// when
		var response = handler.handleUnexpectedError(exception);

		// then
		assertSoftly(softly -> {
			softly.assertThat(response.getStatusCode().value()).isEqualTo(500);
			softly.assertThat(response.getBody().code()).isEqualTo("INTERNAL_ERROR");
			softly.assertThat(response.getBody().message()).doesNotContain("Connection pool");
		});
	}
}
