package com.carrefour.discountkata.discount.domain.model;

import java.util.Objects;

public record Product(String id, String name, Money unitPrice) {

	public Product {
		if (id == null || id.isBlank()) {
			throw new IllegalArgumentException("Product id must not be blank");
		}
		if (name == null || name.isBlank()) {
			throw new IllegalArgumentException("Product name must not be blank");
		}
		Objects.requireNonNull(unitPrice, "unitPrice must not be null");
		if (!unitPrice.isStrictlyPositive()) {
			throw new IllegalArgumentException("Unit price of product '%s' must be strictly positive".formatted(id));
		}
	}
}
