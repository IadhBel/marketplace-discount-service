package com.carrefour.discountkata.discount.adapter.out.persistence.mapper;

import com.carrefour.discountkata.discount.adapter.out.persistence.entity.DiscountCodeEntity;
import com.carrefour.discountkata.discount.adapter.out.persistence.entity.ProductEntity;
import com.carrefour.discountkata.discount.domain.model.DiscountCode;
import com.carrefour.discountkata.discount.domain.model.Money;
import com.carrefour.discountkata.discount.domain.model.Product;
import org.mapstruct.Mapper;

import java.math.BigDecimal;

@Mapper
public interface DiscountPersistenceMapper {

	Product toDomain(ProductEntity entity);

	DiscountCode toDomain(DiscountCodeEntity entity);

	default Money toMoney(BigDecimal amount) {
		return new Money(amount);
	}
}
