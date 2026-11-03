package com.carrefour.discountkata.discount.adapter.out.persistence.adapter;

import com.carrefour.discountkata.discount.adapter.out.persistence.mapper.DiscountPersistenceMapper;
import com.carrefour.discountkata.discount.adapter.out.persistence.repository.DiscountCodeJpaRepository;
import com.carrefour.discountkata.discount.application.port.out.LoadDiscountCodePort;
import com.carrefour.discountkata.discount.domain.model.DiscountCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class DiscountCodePersistenceAdapter implements LoadDiscountCodePort {

	private final DiscountCodeJpaRepository repository;
	private final DiscountPersistenceMapper mapper;

	@Override
	public Optional<DiscountCode> loadByCode(String code) {
		return repository.findById(code).map(mapper::toDomain);
	}
}
