package com.carrefour.discountkata.discount.domain.exception;

public final class DiscountCodeNotFoundException extends DiscountException {

	public DiscountCodeNotFoundException(String code) {
		super("Discount code '%s' does not exist.".formatted(code));
	}
}
