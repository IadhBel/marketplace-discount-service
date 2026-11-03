package com.carrefour.discountkata.discount.domain.model;

import com.carrefour.discountkata.discount.domain.exception.DiscountCodeExpiredException;
import com.carrefour.discountkata.discount.domain.exception.DiscountCodeInactiveException;
import com.carrefour.discountkata.discount.domain.exception.DiscountCodeNotYetValidException;
import com.carrefour.discountkata.support.creator.DiscountCodeCreator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

class DiscountCodeTest {

	private static final Instant STARTS_AT = Instant.parse("2026-06-01T00:00:00Z");
	private static final Instant EXPIRES_AT = Instant.parse("2026-09-01T00:00:00Z");

	@Test
	void shouldTrimAndUpperCase_whenNormalizingCode() {
		// given
		var typedCode = " summer10 ";

		// when
		var normalized = DiscountCode.normalize(typedCode);

		// then
		assertSoftly(softly -> {
			softly.assertThat(normalized).isEqualTo("SUMMER10");
			softly.assertThat(DiscountCode.normalize(null)).isEmpty();
		});
	}

	@Test
	void shouldRejectCode_whenInactive() {
		// given
		var code = DiscountCodeCreator.inactiveCode("DISABLED15");

		// when / then
		assertThatThrownBy(() -> code.assertUsableAt(STARTS_AT)).isInstanceOf(DiscountCodeInactiveException.class);
	}

	@ParameterizedTest
	@ValueSource(strings = {"2026-06-01T00:00:00Z", "2026-08-31T23:59:59Z"})
	void shouldAcceptCode_whenInsideValidityPeriod(String instant) {
		// given
		var code = DiscountCodeCreator.codeValidBetween("PROMO", STARTS_AT, EXPIRES_AT);

		// when / then
		assertThatCode(() -> code.assertUsableAt(Instant.parse(instant))).doesNotThrowAnyException();
	}

	@Test
	void shouldRejectCode_whenBeforeStartDate() {
		// given
		var code = DiscountCodeCreator.codeValidBetween("PROMO", STARTS_AT, EXPIRES_AT);

		// when / then
		assertThatThrownBy(() -> code.assertUsableAt(STARTS_AT.minusSeconds(1)))
				.isInstanceOf(DiscountCodeNotYetValidException.class);
	}

	@Test
	void shouldRejectCode_whenExactlyAtExpiration() {
		// given
		var code = DiscountCodeCreator.codeValidBetween("PROMO", STARTS_AT, EXPIRES_AT);

		// when / then
		assertThatThrownBy(() -> code.assertUsableAt(EXPIRES_AT)).isInstanceOf(DiscountCodeExpiredException.class);
	}

	@ParameterizedTest
	@CsvSource({"'', 10", "BROKEN, 0", "BROKEN, 101"})
	void shouldRejectData_whenCodeIsBlankOrPercentageOutOfRange(String code, int percentage) {
		// given
		var active = true;

		// when / then
		assertThatThrownBy(() -> new DiscountCode(code, percentage, active, null, null))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void shouldRejectData_whenStartDateIsNotBeforeExpiration() {
		// given
		var code = "BROKEN";

		// when / then
		assertThatThrownBy(() -> new DiscountCode(code, 10, true, EXPIRES_AT, STARTS_AT))
				.isInstanceOf(IllegalArgumentException.class);
	}
}
