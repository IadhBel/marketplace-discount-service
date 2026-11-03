package com.carrefour.discountkata.discount.domain.exception;

import java.util.Collection;

public final class ProductNotFoundException extends DiscountException {

	public ProductNotFoundException(Collection<String> missingProductIds) {
		super("Unknown product(s): %s.".formatted(String.join(", ", missingProductIds)));
	}
}
