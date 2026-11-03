package com.carrefour.discountkata.discount.domain.model;

import com.carrefour.discountkata.discount.domain.exception.InvalidCartException;
import com.carrefour.discountkata.support.creator.CartCreator;
import com.carrefour.discountkata.support.creator.ProductCreator;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CartTest {

	@Test
	void shouldRejectCart_whenEmpty() {
		// given
		List<CartItem> noItems = List.of();

		// when / then
		assertThatThrownBy(() -> new Cart(noItems)).isInstanceOf(InvalidCartException.class)
				.hasMessageContaining("at least one item");
	}

	@Test
	void shouldRejectCart_whenProductAppearsTwice() {
		// given
		var first = new CartItem(ProductCreator.coffee(), 1);
		var second = new CartItem(ProductCreator.coffee(), 2);

		// when / then
		assertThatThrownBy(() -> CartCreator.cartOf(first, second)).isInstanceOf(InvalidCartException.class)
				.hasMessageContaining("P-001");
	}
}
