package com.carrefour.discountkata.discount.adapter.in.web.dto;

import java.util.List;

public record ErrorResponse(int status, String code, String message, List<String> details) {

	public ErrorResponse {
		details = details == null ? List.of() : List.copyOf(details);
	}

	public static ErrorResponse of(int status, String code, String message) {
		return new ErrorResponse(status, code, message, List.of());
	}
}
