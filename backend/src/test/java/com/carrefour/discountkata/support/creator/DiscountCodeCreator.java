package com.carrefour.discountkata.support.creator;

import com.carrefour.discountkata.discount.domain.model.DiscountCode;

import java.time.Instant;

public final class DiscountCodeCreator {

	private DiscountCodeCreator() {
	}

	public static DiscountCode summer10() {
		return activeCode("SUMMER10", 10);
	}

	public static DiscountCode activeCode(String code, int percentage) {
		return new DiscountCode(code, percentage, true, null, null);
	}

	public static DiscountCode inactiveCode(String code) {
		return new DiscountCode(code, 15, false, null, null);
	}

	public static DiscountCode codeValidBetween(String code, Instant startsAt, Instant expiresAt) {
		return new DiscountCode(code, 20, true, startsAt, expiresAt);
	}
}
