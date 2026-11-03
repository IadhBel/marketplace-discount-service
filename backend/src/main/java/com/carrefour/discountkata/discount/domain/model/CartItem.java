package com.carrefour.discountkata.discount.domain.model;

import com.carrefour.discountkata.discount.domain.exception.InvalidCartException;

import java.util.Objects;

public record CartItem(Product product, int quantity) {

	public static final int MIN_QUANTITY = 1;
	public static final int MAX_QUANTITY = 99;

	public CartItem {
		Objects.requireNonNull(product, "product must not be null");
		if (quantity < MIN_QUANTITY || quantity > MAX_QUANTITY) {
			throw new InvalidCartException("Quantity of product '%s' must be between %d and %d.".formatted(product.id(),
					MIN_QUANTITY, MAX_QUANTITY));
		}
	}

	public Money lineTotal() {
		return product.unitPrice().multiply(quantity);
	}
}
