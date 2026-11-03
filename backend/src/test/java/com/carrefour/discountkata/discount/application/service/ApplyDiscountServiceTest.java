package com.carrefour.discountkata.discount.application.service;

import com.carrefour.discountkata.discount.application.command.ApplyDiscountCommand;
import com.carrefour.discountkata.discount.application.command.CartItemCommand;
import com.carrefour.discountkata.discount.application.port.out.LoadDiscountCodePort;
import com.carrefour.discountkata.discount.application.port.out.LoadProductsPort;
import com.carrefour.discountkata.discount.application.result.ApplyDiscountResult;
import com.carrefour.discountkata.discount.domain.exception.DiscountCodeNotFoundException;
import com.carrefour.discountkata.discount.domain.exception.ProductNotFoundException;
import com.carrefour.discountkata.discount.domain.model.Money;
import com.carrefour.discountkata.discount.domain.policy.DiscountPolicy;
import com.carrefour.discountkata.support.creator.DiscountCodeCreator;
import com.carrefour.discountkata.support.creator.ProductCreator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class ApplyDiscountServiceTest {

	private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-11T10:00:00Z"), ZoneOffset.UTC);

	@Mock
	private LoadDiscountCodePort loadDiscountCodePort;

	@Mock
	private LoadProductsPort loadProductsPort;

	private ApplyDiscountService service;

	@BeforeEach
	void setUp() {
		service = new ApplyDiscountService(loadDiscountCodePort, loadProductsPort, new DiscountPolicy(CLOCK));
	}

	@Test
	void shouldReturnDiscountedCart_whenCodeAndProductsExist() {
		// given
		var command = new ApplyDiscountCommand(" summer10 ",
				List.of(new CartItemCommand("P-001", 2), new CartItemCommand("P-003", 1)));
		given(loadDiscountCodePort.loadByCode("SUMMER10")).willReturn(Optional.of(DiscountCodeCreator.summer10()));
		given(loadProductsPort.loadByIds(anyCollection()))
				.willReturn(List.of(ProductCreator.coffee(), ProductCreator.baguette()));

		// when
		ApplyDiscountResult result = service.apply(command);

		// then
		assertSoftly(softly -> {
			softly.assertThat(result.code()).isEqualTo("SUMMER10");
			softly.assertThat(result.discountPercentage()).isEqualTo(10);
			softly.assertThat(result.currency()).isEqualTo("EUR");
			softly.assertThat(result.lines()).extracting(ApplyDiscountResult.Line::productId).containsExactly("P-001",
					"P-003");
			softly.assertThat(result.subtotal()).isEqualTo(Money.of("10.48"));
			softly.assertThat(result.discountAmount()).isEqualTo(Money.of("1.05"));
			softly.assertThat(result.total()).isEqualTo(Money.of("9.43"));
		});
		verify(loadDiscountCodePort).loadByCode("SUMMER10");
		verify(loadProductsPort).loadByIds(Set.of("P-001", "P-003"));
	}

	@Test
	void shouldRejectCode_whenCodeIsUnknown() {
		// given
		var command = new ApplyDiscountCommand("UNKNOWN", List.of(new CartItemCommand("P-001", 1)));
		given(loadDiscountCodePort.loadByCode("UNKNOWN")).willReturn(Optional.empty());

		// when / then
		assertThatThrownBy(() -> service.apply(command)).isInstanceOf(DiscountCodeNotFoundException.class)
				.hasMessageContaining("UNKNOWN");
		verifyNoInteractions(loadProductsPort);
	}

	@Test
	void shouldRejectCart_whenProductIsUnknown() {
		// given
		var command = new ApplyDiscountCommand("SUMMER10",
				List.of(new CartItemCommand("P-001", 1), new CartItemCommand("P-999", 1)));
		given(loadDiscountCodePort.loadByCode("SUMMER10")).willReturn(Optional.of(DiscountCodeCreator.summer10()));
		given(loadProductsPort.loadByIds(anyCollection())).willReturn(List.of(ProductCreator.coffee()));

		// when / then
		assertThatThrownBy(() -> service.apply(command)).isInstanceOf(ProductNotFoundException.class)
				.hasMessageContaining("P-999");
		verify(loadProductsPort).loadByIds(Set.of("P-001", "P-999"));
	}
}
