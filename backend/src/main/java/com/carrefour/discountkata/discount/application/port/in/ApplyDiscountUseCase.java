package com.carrefour.discountkata.discount.application.port.in;

import com.carrefour.discountkata.discount.application.command.ApplyDiscountCommand;
import com.carrefour.discountkata.discount.application.result.ApplyDiscountResult;

public interface ApplyDiscountUseCase {

	ApplyDiscountResult apply(ApplyDiscountCommand command);
}
