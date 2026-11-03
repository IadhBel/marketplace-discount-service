package com.carrefour.discountkata.discount.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.SoftAssertions.assertSoftly;

class MoneyTest {

	@Test
	void shouldRoundHalfUpToTwoDecimals_whenCreated() {
		// given
		var lowerHalf = "0.124";
		var upperHalf = "0.125";
		var oneDecimal = "2.5";

		// when
		var roundedDown = Money.of(lowerHalf);
		var roundedUp = Money.of(upperHalf);
		var normalized = Money.of(oneDecimal);

		// then
		assertSoftly(softly -> {
			softly.assertThat(roundedDown).isEqualTo(Money.of("0.12"));
			softly.assertThat(roundedUp).isEqualTo(Money.of("0.13"));
			softly.assertThat(normalized).isEqualTo(Money.of("2.50"));
		});
	}
}
