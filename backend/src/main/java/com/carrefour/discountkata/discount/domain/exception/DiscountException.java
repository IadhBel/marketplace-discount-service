package com.carrefour.discountkata.discount.domain.exception;

/**
 * Root of the business errors of the discount domain. Sealed so that the web
 * adapter maps every case to an HTTP status exhaustively, with no default
 * branch.
 */
public abstract sealed class DiscountException extends RuntimeException
		permits DiscountCodeNotFoundException, DiscountCodeInactiveException, DiscountCodeNotYetValidException,
		DiscountCodeExpiredException, ProductNotFoundException, InvalidCartException {

	protected DiscountException(String message) {
		super(message);
	}
}
