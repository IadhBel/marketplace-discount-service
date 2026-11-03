package com.carrefour.discountkata.discount.adapter.in.web.mapper;

import com.carrefour.discountkata.discount.adapter.in.web.dto.ApplyDiscountRequest;
import com.carrefour.discountkata.discount.adapter.in.web.dto.ApplyDiscountResponse;
import com.carrefour.discountkata.discount.application.command.ApplyDiscountCommand;
import com.carrefour.discountkata.discount.application.result.ApplyDiscountResult;
import com.carrefour.discountkata.discount.domain.model.Money;
import org.mapstruct.Mapper;

import java.math.BigDecimal;

@Mapper
public interface DiscountWebMapper {

	ApplyDiscountCommand toCommand(ApplyDiscountRequest request);

	ApplyDiscountResponse toResponse(ApplyDiscountResult result);

	default BigDecimal toAmount(Money money) {
		return money.amount();
	}
}
