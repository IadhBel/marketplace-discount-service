package com.carrefour.discountkata.discount.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "discount_code")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DiscountCodeEntity {

	@Id
	private String code;

	@Column(nullable = false)
	private int percentage;

	@Column(nullable = false)
	private boolean active;

	@Column(name = "starts_at")
	private Instant startsAt;

	@Column(name = "expires_at")
	private Instant expiresAt;
}
