package com.carrefour.discountkata.discount.adapter.out.persistence.adapter;

import com.carrefour.discountkata.discount.adapter.out.persistence.mapper.DiscountPersistenceMapper;
import com.carrefour.discountkata.discount.adapter.out.persistence.repository.ProductJpaRepository;
import com.carrefour.discountkata.discount.application.port.out.LoadProductsPort;
import com.carrefour.discountkata.discount.domain.model.Product;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ProductPersistenceAdapter implements LoadProductsPort {

	private final ProductJpaRepository repository;
	private final DiscountPersistenceMapper mapper;

	@Override
	public List<Product> loadByIds(Collection<String> productIds) {
		return repository.findAllById(productIds).stream().map(mapper::toDomain).toList();
	}
}
