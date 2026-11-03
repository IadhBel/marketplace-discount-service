package com.carrefour.discountkata.support.creator;

import com.carrefour.discountkata.discount.domain.model.Cart;
import com.carrefour.discountkata.discount.domain.model.CartItem;

import java.util.List;

public final class CartCreator {

	private CartCreator() {
	}

	/**
	 * 2 × coffee (7.98) + 1 × baguette (2.50): subtotal 10.48.
	 */
	public static Cart coffeeAndBaguette() {
		return new Cart(List.of(new CartItem(ProductCreator.coffee(), 2), new CartItem(ProductCreator.baguette(), 1)));
	}

	public static Cart cartOf(CartItem... items) {
		return new Cart(List.of(items));
	}
}
