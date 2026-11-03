package com.carrefour.discountkata.discount.application.port.out;

import com.carrefour.discountkata.discount.domain.model.DiscountCode;

import java.util.Optional;

public interface LoadDiscountCodePort {

	/**
	 * @param code
	 *            the code in its normalized form (see
	 *            {@link DiscountCode#normalize(String)})
	 */
	Optional<DiscountCode> loadByCode(String code);
}
