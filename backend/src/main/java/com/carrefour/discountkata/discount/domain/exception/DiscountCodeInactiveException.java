package com.carrefour.discountkata.discount.domain.exception;

public final class DiscountCodeInactiveException extends DiscountException {

	public DiscountCodeInactiveException(String code) {
		super("Discount code '%s' is not active.".formatted(code));
	}
}
