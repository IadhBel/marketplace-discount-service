package com.carrefour.discountkata.discount.adapter.in.web.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ApplyDiscountRequestValidationTest {

	private static ValidatorFactory validatorFactory;
	private static Validator validator;

	@BeforeAll
	static void createValidator() {
		validatorFactory = Validation.buildDefaultValidatorFactory();
		validator = validatorFactory.getValidator();
	}

	@AfterAll
	static void closeValidator() {
		validatorFactory.close();
	}

	@Test
	void shouldAcceptRequest_whenCodeAndItemsAreValid() {
		// given
		var request = new ApplyDiscountRequest("SUMMER10", List.of(new CartItemRequest("P-001", 2)));

		// when
		var violations = validator.validate(request);

		// then
		assertThat(violations).isEmpty();
	}

	@Test
	void shouldRejectRequest_whenCodeIsBlank() {
		// given
		var request = new ApplyDiscountRequest("  ", List.of(new CartItemRequest("P-001", 1)));

		// when
		var violations = validator.validate(request);

		// then
		assertThat(violations).extracting(violation -> violation.getPropertyPath().toString()).containsExactly("code");
	}

	@Test
	void shouldRejectRequest_whenCartIsEmpty() {
		// given
		var request = new ApplyDiscountRequest("SUMMER10", List.of());

		// when
		var violations = validator.validate(request);

		// then
		assertThat(violations).extracting(violation -> violation.getPropertyPath().toString()).containsExactly("items");
	}

	@Test
	void shouldRejectRequest_whenItemIsNull() {
		// given
		var request = new ApplyDiscountRequest("SUMMER10", Arrays.asList((CartItemRequest) null));

		// when
		var violations = validator.validate(request);

		// then
		assertThat(violations).extracting(violation -> violation.getPropertyPath().toString())
				.containsExactly("items[0].<list element>");
	}

	@Test
	void shouldRejectRequest_whenProductIdIsBlank() {
		// given
		var request = new ApplyDiscountRequest("SUMMER10", List.of(new CartItemRequest(" ", 1)));

		// when
		var violations = validator.validate(request);

		// then
		assertThat(violations).extracting(violation -> violation.getPropertyPath().toString())
				.containsExactly("items[0].productId");
	}

	@ParameterizedTest
	@NullSource
	@ValueSource(ints = {0, 100})
	void shouldRejectRequest_whenQuantityIsMissingOrOutOfRange(Integer quantity) {
		// given
		var request = new ApplyDiscountRequest("SUMMER10", List.of(new CartItemRequest("P-001", quantity)));

		// when
		var violations = validator.validate(request);

		// then
		assertThat(violations).extracting(violation -> violation.getPropertyPath().toString())
				.containsExactly("items[0].quantity");
	}
}
