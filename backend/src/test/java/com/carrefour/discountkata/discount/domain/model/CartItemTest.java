package com.carrefour.discountkata.discount.domain.model;

import com.carrefour.discountkata.discount.domain.exception.InvalidCartException;
import com.carrefour.discountkata.support.creator.ProductCreator;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CartItemTest {

	@ParameterizedTest
	@ValueSource(ints = {1, 99})
	void shouldAcceptQuantity_whenAtBounds(int quantity) {
		// given
		var product = ProductCreator.coffee();

		// when / then
		assertThatCode(() -> new CartItem(product, quantity)).doesNotThrowAnyException();
	}

	@ParameterizedTest
	@ValueSource(ints = {0, 100})
	void shouldRejectQuantity_whenOutsideBounds(int quantity) {
		// given
		var product = ProductCreator.coffee();

		// when / then
		assertThatThrownBy(() -> new CartItem(product, quantity)).isInstanceOf(InvalidCartException.class)
				.hasMessageContaining("between 1 and 99");
	}
}
