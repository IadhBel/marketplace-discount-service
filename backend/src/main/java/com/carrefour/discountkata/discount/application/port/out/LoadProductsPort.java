package com.carrefour.discountkata.discount.application.port.out;

import com.carrefour.discountkata.discount.domain.model.Product;

import java.util.Collection;
import java.util.List;

public interface LoadProductsPort {

	/**
	 * Loads the requested products in a single call. Unknown identifiers are absent
	 * from the result: the caller detects them.
	 */
	List<Product> loadByIds(Collection<String> productIds);
}
