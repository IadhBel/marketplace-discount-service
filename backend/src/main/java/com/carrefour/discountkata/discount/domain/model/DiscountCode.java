package com.carrefour.discountkata.discount.domain.model;

import com.carrefour.discountkata.discount.domain.exception.DiscountCodeExpiredException;
import com.carrefour.discountkata.discount.domain.exception.DiscountCodeInactiveException;
import com.carrefour.discountkata.discount.domain.exception.DiscountCodeNotYetValidException;

import java.time.Instant;
import java.util.Locale;

public record DiscountCode(String code, int percentage, boolean active, Instant startsAt, Instant expiresAt) {

	public static final int MIN_PERCENTAGE = 1;
	public static final int MAX_PERCENTAGE = 100;

	public DiscountCode {
		if (code == null || code.isBlank()) {
			throw new IllegalArgumentException("Discount code must not be blank");
		}
		if (percentage < MIN_PERCENTAGE || percentage > MAX_PERCENTAGE) {
			throw new IllegalArgumentException("Percentage of code '%s' must be between %d and %d".formatted(code,
					MIN_PERCENTAGE, MAX_PERCENTAGE));
		}
		if (startsAt != null && expiresAt != null && !startsAt.isBefore(expiresAt)) {
			throw new IllegalArgumentException("Code '%s' must start before it expires".formatted(code));
		}
	}

	public static String normalize(String rawCode) {
		return rawCode == null ? "" : rawCode.trim().toUpperCase(Locale.ROOT);
	}

	/**
	 * Checks that the code can be used at the given instant: active, and inside its
	 * validity period (start inclusive, expiration exclusive).
	 */
	public void assertUsableAt(Instant now) {
		if (!active) {
			throw new DiscountCodeInactiveException(code);
		}
		if (startsAt != null && now.isBefore(startsAt)) {
			throw new DiscountCodeNotYetValidException(code);
		}
		if (expiresAt != null && !now.isBefore(expiresAt)) {
			throw new DiscountCodeExpiredException(code);
		}
	}
}
