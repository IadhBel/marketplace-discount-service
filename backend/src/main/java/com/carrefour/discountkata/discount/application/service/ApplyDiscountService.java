package com.carrefour.discountkata.discount.application.service;

import com.carrefour.discountkata.discount.application.command.ApplyDiscountCommand;
import com.carrefour.discountkata.discount.application.command.CartItemCommand;
import com.carrefour.discountkata.discount.application.port.in.ApplyDiscountUseCase;
import com.carrefour.discountkata.discount.application.port.out.LoadDiscountCodePort;
import com.carrefour.discountkata.discount.application.port.out.LoadProductsPort;
import com.carrefour.discountkata.discount.application.result.ApplyDiscountResult;
import com.carrefour.discountkata.discount.domain.exception.DiscountCodeNotFoundException;
import com.carrefour.discountkata.discount.domain.exception.ProductNotFoundException;
import com.carrefour.discountkata.discount.domain.model.Cart;
import com.carrefour.discountkata.discount.domain.model.CartItem;
import com.carrefour.discountkata.discount.domain.model.DiscountCode;
import com.carrefour.discountkata.discount.domain.model.DiscountResult;
import com.carrefour.discountkata.discount.domain.model.Product;
import com.carrefour.discountkata.discount.domain.policy.DiscountPolicy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
public class ApplyDiscountService implements ApplyDiscountUseCase {

	private final LoadDiscountCodePort loadDiscountCodePort;
	private final LoadProductsPort loadProductsPort;
	private final DiscountPolicy discountPolicy;

	@Override
	public ApplyDiscountResult apply(ApplyDiscountCommand command) {
		String code = DiscountCode.normalize(command.code());
		log.debug("Applying discount code '{}' to a cart of {} line(s)", code, command.items().size());

		DiscountCode discountCode = loadDiscountCodePort.loadByCode(code)
				.orElseThrow(() -> new DiscountCodeNotFoundException(code));
		Cart cart = buildCart(command.items());

		DiscountResult result = discountPolicy.apply(cart, discountCode);

		log.info("Discount code '{}' applied: subtotal={}, discount={}, total={}", code, result.subtotal().amount(),
				result.discountAmount().amount(), result.total().amount());
		return ApplyDiscountResult.from(result);
	}

	private Cart buildCart(List<CartItemCommand> itemCommands) {
		Set<String> requestedIds = itemCommands.stream().map(CartItemCommand::productId)
				.collect(Collectors.toCollection(LinkedHashSet::new));

		Map<String, Product> productsById = loadProductsPort.loadByIds(requestedIds).stream()
				.collect(Collectors.toMap(Product::id, Function.identity()));

		var missingIds = new LinkedHashSet<>(requestedIds);
		missingIds.removeAll(productsById.keySet());
		if (!missingIds.isEmpty()) {
			throw new ProductNotFoundException(missingIds);
		}

		return new Cart(itemCommands.stream()
				.map(item -> new CartItem(productsById.get(item.productId()), item.quantity())).toList());
	}
}
