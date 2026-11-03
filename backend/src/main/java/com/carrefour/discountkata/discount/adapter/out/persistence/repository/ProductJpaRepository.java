package com.carrefour.discountkata.discount.adapter.out.persistence.repository;

import com.carrefour.discountkata.discount.adapter.out.persistence.entity.ProductEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductJpaRepository extends JpaRepository<ProductEntity, String> {
}
