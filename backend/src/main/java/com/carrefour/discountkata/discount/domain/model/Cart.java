package com.carrefour.discountkata.discount.domain.model;

import com.carrefour.discountkata.discount.domain.exception.InvalidCartException;

import java.util.HashSet;
import java.util.List;

public record Cart(List<CartItem> items) {

	public Cart {
		if (items == null || items.isEmpty()) {
			throw new InvalidCartException("The cart must contain at least one item.");
		}
		items = List.copyOf(items);
		var productIds = new HashSet<String>();
		for (var item : items) {
			if (!productIds.add(item.product().id())) {
				throw new InvalidCartException(
						"Product '%s' appears more than once in the cart.".formatted(item.product().id()));
			}
		}
	}

	public Money subtotal() {
		return items.stream().map(CartItem::lineTotal).reduce(Money.ZERO, Money::add);
	}
}
