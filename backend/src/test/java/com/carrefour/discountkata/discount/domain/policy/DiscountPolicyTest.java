package com.carrefour.discountkata.discount.domain.policy;

import com.carrefour.discountkata.discount.domain.exception.DiscountCodeExpiredException;
import com.carrefour.discountkata.discount.domain.model.CartItem;
import com.carrefour.discountkata.discount.domain.model.DiscountResult;
import com.carrefour.discountkata.discount.domain.model.Money;
import com.carrefour.discountkata.support.creator.CartCreator;
import com.carrefour.discountkata.support.creator.DiscountCodeCreator;
import com.carrefour.discountkata.support.creator.ProductCreator;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

class DiscountPolicyTest {

	private static final Instant NOW = Instant.parse("2026-10-11T10:00:00Z");

	private final DiscountPolicy policy = new DiscountPolicy(Clock.fixed(NOW, ZoneOffset.UTC));

	@Test
	void shouldApplyPercentageToSubtotal_whenCodeIsUsable() {
		// given
		var cart = CartCreator.coffeeAndBaguette();
		var code = DiscountCodeCreator.summer10();

		// when
		DiscountResult result = policy.apply(cart, code);

		// then
		assertSoftly(softly -> {
			softly.assertThat(result.subtotal()).isEqualTo(Money.of("10.48"));
			softly.assertThat(result.discountAmount()).isEqualTo(Money.of("1.05"));
			softly.assertThat(result.total()).isEqualTo(Money.of("9.43"));
			softly.assertThat(result.lines()).hasSize(2);
			softly.assertThat(result.discountCode()).isEqualTo(code);
		});
	}

	@Test
	void shouldRoundDiscountHalfUp_whenDiscountHasMoreThanTwoDecimals() {
		// given
		var cart = CartCreator.cartOf(new CartItem(ProductCreator.productPriced("0.05"), 1));
		var code = DiscountCodeCreator.activeCode("TEN", 10);

		// when
		DiscountResult result = policy.apply(cart, code);

		// then
		assertThat(result.discountAmount()).isEqualTo(Money.of("0.01"));
	}

	@Test
	void shouldGiveZeroTotal_whenPercentageIsOneHundred() {
		// given
		var cart = CartCreator.coffeeAndBaguette();
		var code = DiscountCodeCreator.activeCode("FREE", 100);

		// when
		DiscountResult result = policy.apply(cart, code);

		// then
		assertSoftly(softly -> {
			softly.assertThat(result.discountAmount()).isEqualTo(Money.of("10.48"));
			softly.assertThat(result.total()).isEqualTo(Money.ZERO);
		});
	}

	@Test
	void shouldRejectCode_whenExpiredAtCurrentDate() {
		// given
		var cart = CartCreator.coffeeAndBaguette();
		var code = DiscountCodeCreator.codeValidBetween("OLD", NOW.minusSeconds(3600), NOW);

		// when / then
		assertThatThrownBy(() -> policy.apply(cart, code)).isInstanceOf(DiscountCodeExpiredException.class);
	}
}
