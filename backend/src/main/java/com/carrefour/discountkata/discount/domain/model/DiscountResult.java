package com.carrefour.discountkata.discount.domain.model;

import java.util.List;

public record DiscountResult(DiscountCode discountCode, List<CartItem> lines, Money subtotal, Money discountAmount,
		Money total) {

	public DiscountResult {
		lines = List.copyOf(lines);
	}
}
