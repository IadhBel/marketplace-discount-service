package com.carrefour.discountkata.discount.domain.model;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductTest {

	@ParameterizedTest
	@CsvSource({"'', Coffee, 3.99", "P-001, '', 3.99", "P-001, Coffee, 0.00", "P-001, Coffee, -1.00"})
	void shouldRejectCatalogData_whenProductIsInvalid(String id, String name, String unitPrice) {
		// given
		var price = Money.of(unitPrice);

		// when / then
		assertThatThrownBy(() -> new Product(id, name, price)).isInstanceOf(IllegalArgumentException.class);
	}
}
