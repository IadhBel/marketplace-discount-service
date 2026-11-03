package com.carrefour.discountkata.discount.application.command;

import java.util.List;

public record ApplyDiscountCommand(String code, List<CartItemCommand> items) {

	public ApplyDiscountCommand {
		items = items == null ? List.of() : List.copyOf(items);
	}
}
