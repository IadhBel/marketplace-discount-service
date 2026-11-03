package com.carrefour.discountkata.discount.adapter.in.web.dto;

import java.math.BigDecimal;
import java.util.List;

public record ApplyDiscountResponse(String code, int discountPercentage, String currency, List<Line> lines,
		BigDecimal subtotal, BigDecimal discountAmount, BigDecimal total) {

	public record Line(String productId, String name, BigDecimal unitPrice, int quantity, BigDecimal lineTotal) {
	}
}
