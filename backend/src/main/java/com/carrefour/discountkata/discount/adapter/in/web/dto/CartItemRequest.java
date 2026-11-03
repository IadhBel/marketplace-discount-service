package com.carrefour.discountkata.discount.adapter.in.web.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CartItemRequest(@NotBlank String productId, @NotNull @Min(1) @Max(99) Integer quantity) {
}
