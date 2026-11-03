package com.carrefour.discountkata.discount.adapter.in.web;

import com.carrefour.discountkata.discount.adapter.in.web.dto.ApplyDiscountRequest;
import com.carrefour.discountkata.discount.adapter.in.web.dto.ApplyDiscountResponse;
import com.carrefour.discountkata.discount.adapter.in.web.mapper.DiscountWebMapper;
import com.carrefour.discountkata.discount.application.port.in.ApplyDiscountUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/discounts")
@RequiredArgsConstructor
public class DiscountController {

	private final ApplyDiscountUseCase applyDiscountUseCase;
	private final DiscountWebMapper mapper;

	@PostMapping("/apply")
	public ApplyDiscountResponse apply(@Valid @RequestBody ApplyDiscountRequest request) {
		return mapper.toResponse(applyDiscountUseCase.apply(mapper.toCommand(request)));
	}
}
