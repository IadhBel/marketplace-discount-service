package com.carrefour.discountkata.support.creator;

import com.carrefour.discountkata.discount.domain.model.Money;
import com.carrefour.discountkata.discount.domain.model.Product;

/**
 * Products of the demo catalog (same values as the Flyway demo data), so that
 * expected amounts stay readable.
 */
public final class ProductCreator {

	private ProductCreator() {
	}

	public static Product coffee() {
		return new Product("P-001", "Ground coffee 250 g", Money.of("3.99"));
	}

	public static Product milk() {
		return new Product("P-002", "Semi-skimmed milk 1 L", Money.of("1.09"));
	}

	public static Product baguette() {
		return new Product("P-003", "Traditional baguette", Money.of("2.50"));
	}

	public static Product productPriced(String unitPrice) {
		return new Product("P-100", "Test product", Money.of(unitPrice));
	}
}
