package com.carrefour.discountkata.discount.adapter.in.web;

import com.carrefour.discountkata.discount.adapter.in.web.mapper.DiscountWebMapperImpl;
import com.carrefour.discountkata.discount.application.command.ApplyDiscountCommand;
import com.carrefour.discountkata.discount.application.port.in.ApplyDiscountUseCase;
import com.carrefour.discountkata.discount.application.result.ApplyDiscountResult;
import com.carrefour.discountkata.discount.domain.policy.DiscountPolicy;
import com.carrefour.discountkata.shared.config.SecurityConfiguration;
import com.carrefour.discountkata.shared.config.SecurityErrorHandler;
import com.carrefour.discountkata.support.JsonFixtures;
import com.carrefour.discountkata.support.creator.CartCreator;
import com.carrefour.discountkata.support.creator.DiscountCodeCreator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.Clock;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DiscountController.class)
@Import({SecurityConfiguration.class, SecurityErrorHandler.class, DiscountWebMapperImpl.class})
// The issuer is never called: the JwtDecoder is mocked
@TestPropertySource(properties = "spring.security.oauth2.resourceserver.jwt.issuer-uri=http://issuer.test.invalid")
class DiscountControllerSecurityTest {

	private static final String APPLY_URL = "/api/discounts/apply";

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private ApplyDiscountUseCase applyDiscountUseCase;

	@MockitoBean
	private JwtDecoder jwtDecoder;

	@Test
	void shouldReturn401_whenNoToken() throws Exception {
		// given
		String body = JsonFixtures.read("apply-discount/request-summer10.json");

		// when
		ResultActions response = mockMvc.perform(post(APPLY_URL).contentType(MediaType.APPLICATION_JSON).content(body));

		// then
		response.andExpect(status().isUnauthorized()).andExpect(header().exists(HttpHeaders.WWW_AUTHENTICATE))
				.andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
		verifyNoInteractions(applyDiscountUseCase);
	}

	@Test
	void shouldReturn401_whenTokenIsInvalid() throws Exception {
		// given
		String body = JsonFixtures.read("apply-discount/request-summer10.json");
		given(jwtDecoder.decode(anyString())).willThrow(new BadJwtException("Signed JWT rejected"));

		// when
		ResultActions response = mockMvc.perform(post(APPLY_URL).header(HttpHeaders.AUTHORIZATION, "Bearer invalid")
				.contentType(MediaType.APPLICATION_JSON).content(body));

		// then
		response.andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
		verifyNoInteractions(applyDiscountUseCase);
	}

	@Test
	void shouldReturn403_whenCustomerRoleIsMissing() throws Exception {
		// given
		String body = JsonFixtures.read("apply-discount/request-summer10.json");

		// when
		ResultActions response = mockMvc
				.perform(post(APPLY_URL).with(jwt()).contentType(MediaType.APPLICATION_JSON).content(body));

		// then
		response.andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("FORBIDDEN"));
		verifyNoInteractions(applyDiscountUseCase);
	}

	@Test
	void shouldProcessRequest_whenCustomerRoleIsPresent() throws Exception {
		// given
		String body = JsonFixtures.read("apply-discount/request-summer10.json");
		var result = ApplyDiscountResult.from(new DiscountPolicy(Clock.systemUTC())
				.apply(CartCreator.coffeeAndBaguette(), DiscountCodeCreator.summer10()));
		given(applyDiscountUseCase.apply(any(ApplyDiscountCommand.class))).willReturn(result);

		// when
		ResultActions response = mockMvc
				.perform(post(APPLY_URL).with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CUSTOMER")))
						.contentType(MediaType.APPLICATION_JSON).content(body));

		// then
		response.andExpect(status().isOk()).andExpect(jsonPath("$.total").value(9.43));
		verify(applyDiscountUseCase).apply(any(ApplyDiscountCommand.class));
	}
}
