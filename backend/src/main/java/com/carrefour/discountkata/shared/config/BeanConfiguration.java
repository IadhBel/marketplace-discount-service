package com.carrefour.discountkata.shared.config;

import com.carrefour.discountkata.discount.application.port.in.ApplyDiscountUseCase;
import com.carrefour.discountkata.discount.application.port.out.LoadDiscountCodePort;
import com.carrefour.discountkata.discount.application.port.out.LoadProductsPort;
import com.carrefour.discountkata.discount.application.service.ApplyDiscountService;
import com.carrefour.discountkata.discount.domain.policy.DiscountPolicy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Wires the framework-free domain and application classes, which carry no
 * Spring annotation.
 */
@Configuration
public class BeanConfiguration {

	@Bean
	public Clock clock() {
		return Clock.systemUTC();
	}

	@Bean
	public DiscountPolicy discountPolicy(Clock clock) {
		return new DiscountPolicy(clock);
	}

	@Bean
	public ApplyDiscountUseCase applyDiscountUseCase(LoadDiscountCodePort loadDiscountCodePort,
			LoadProductsPort loadProductsPort, DiscountPolicy discountPolicy) {
		return new ApplyDiscountService(loadDiscountCodePort, loadProductsPort, discountPolicy);
	}
}
