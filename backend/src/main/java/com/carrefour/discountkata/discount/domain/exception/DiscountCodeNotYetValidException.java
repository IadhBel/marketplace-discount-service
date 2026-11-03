package com.carrefour.discountkata.discount.domain.exception;

public final class DiscountCodeNotYetValidException extends DiscountException {

	public DiscountCodeNotYetValidException(String code) {
		super("Discount code '%s' is not valid yet.".formatted(code));
	}
}
