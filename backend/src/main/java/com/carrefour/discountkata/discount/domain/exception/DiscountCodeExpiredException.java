package com.carrefour.discountkata.discount.domain.exception;

public final class DiscountCodeExpiredException extends DiscountException {

	public DiscountCodeExpiredException(String code) {
		super("Discount code '%s' is not valid at this date.".formatted(code));
	}
}
