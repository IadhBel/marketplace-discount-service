package com.carrefour.discountkata.discount.domain.policy;

import com.carrefour.discountkata.discount.domain.model.Cart;
import com.carrefour.discountkata.discount.domain.model.DiscountCode;
import com.carrefour.discountkata.discount.domain.model.DiscountResult;
import com.carrefour.discountkata.discount.domain.model.Money;

import java.time.Clock;

public class DiscountPolicy {

	private final Clock clock;

	public DiscountPolicy(Clock clock) {
		this.clock = clock;
	}

	public DiscountResult apply(Cart cart, DiscountCode discountCode) {
		discountCode.assertUsableAt(clock.instant());

		Money subtotal = cart.subtotal();
		Money discountAmount = subtotal.percentage(discountCode.percentage());
		Money total = subtotal.subtract(discountAmount);

		return new DiscountResult(discountCode, cart.items(), subtotal, discountAmount, total);
	}
}
