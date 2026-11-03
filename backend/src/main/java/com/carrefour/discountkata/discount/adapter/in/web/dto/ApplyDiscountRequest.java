package com.carrefour.discountkata.discount.adapter.in.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record ApplyDiscountRequest(@NotBlank String code, @NotEmpty List<@Valid @NotNull CartItemRequest> items) {
}
