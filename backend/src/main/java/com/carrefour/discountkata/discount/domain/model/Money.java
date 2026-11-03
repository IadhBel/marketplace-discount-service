package com.carrefour.discountkata.discount.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Amount in the single currency of the application (EUR). Always held with 2
 * decimals, rounded HALF_UP, so that two equal amounts are always
 * {@code equals}.
 */
public record Money(BigDecimal amount) {

	private static final int SCALE = 2;
	private static final RoundingMode ROUNDING = RoundingMode.HALF_UP;
	private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);

	public static final Money ZERO = new Money(BigDecimal.ZERO);

	public Money {
		Objects.requireNonNull(amount, "amount must not be null");
		amount = amount.setScale(SCALE, ROUNDING);
	}

	public static Money of(String amount) {
		return new Money(new BigDecimal(amount));
	}

	public Money add(Money other) {
		return new Money(amount.add(other.amount));
	}

	public Money subtract(Money other) {
		return new Money(amount.subtract(other.amount));
	}

	public Money multiply(int factor) {
		return new Money(amount.multiply(BigDecimal.valueOf(factor)));
	}

	/**
	 * Computes {@code percentage}% of this amount, rounded once HALF_UP to 2
	 * decimals (RG-14).
	 */
	public Money percentage(int percentage) {
		return new Money(amount.multiply(BigDecimal.valueOf(percentage)).divide(ONE_HUNDRED, SCALE, ROUNDING));
	}

	public boolean isStrictlyPositive() {
		return amount.signum() > 0;
	}
}
