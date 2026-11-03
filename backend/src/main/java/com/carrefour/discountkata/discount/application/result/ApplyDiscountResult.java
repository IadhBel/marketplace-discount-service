package com.carrefour.discountkata.discount.application.result;

import com.carrefour.discountkata.discount.domain.model.DiscountResult;
import com.carrefour.discountkata.discount.domain.model.Money;

import java.util.List;

public record ApplyDiscountResult(String code, int discountPercentage, String currency, List<Line> lines,
		Money subtotal, Money discountAmount, Money total) {

	private static final String CURRENCY = "EUR";

	public ApplyDiscountResult {
		lines = List.copyOf(lines);
	}

	public static ApplyDiscountResult from(DiscountResult result) {
		List<Line> lines = result.lines().stream().map(item -> new Line(item.product().id(), item.product().name(),
				item.product().unitPrice(), item.quantity(), item.lineTotal())).toList();

		return new ApplyDiscountResult(result.discountCode().code(), result.discountCode().percentage(), CURRENCY,
				lines, result.subtotal(), result.discountAmount(), result.total());
	}

	public record Line(String productId, String name, Money unitPrice, int quantity, Money lineTotal) {
	}
}
