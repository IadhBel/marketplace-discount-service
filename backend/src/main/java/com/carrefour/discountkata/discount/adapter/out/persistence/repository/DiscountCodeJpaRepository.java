package com.carrefour.discountkata.discount.adapter.out.persistence.repository;

import com.carrefour.discountkata.discount.adapter.out.persistence.entity.DiscountCodeEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DiscountCodeJpaRepository extends JpaRepository<DiscountCodeEntity, String> {
}
