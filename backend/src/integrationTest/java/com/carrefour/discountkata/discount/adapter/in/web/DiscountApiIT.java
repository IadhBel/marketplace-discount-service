package com.carrefour.discountkata.discount.adapter.in.web;

import com.carrefour.discountkata.support.JsonFixtures;
import com.carrefour.discountkata.support.PostgresTestConfiguration;
import com.carrefour.discountkata.support.KeycloakTestConfiguration;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.stream.Stream;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import({PostgresTestConfiguration.class, KeycloakTestConfiguration.class})
@ActiveProfiles("it")
class DiscountApiIT {

	private static final String APPLY_URL = "/api/discounts/apply";

	@Value("${local.server.port}")
	private int port;

	@Autowired
	private KeycloakTestConfiguration keycloak;

	@BeforeEach
	void configureRestAssured() {
		RestAssured.port = port;
	}

	@Test
	void shouldApplyDiscount_whenCustomerSendsValidCart() throws Exception {
		// given
		String request = JsonFixtures.read("apply-discount/request-summer10.json");
		String expected = JsonFixtures.read("apply-discount/response-summer10.json");

		// when
		Response response = as(Caller.CUSTOMER).body(request).post(APPLY_URL);

		// then
		assertSoftly(softly -> softly.assertThat(response.statusCode()).isEqualTo(200));
		JSONAssert.assertEquals(expected, response.asString(), JSONCompareMode.STRICT);
	}

	static Stream<Arguments> rejectedRequests() {
		return Stream.of(
				Arguments.of("unknown code", "request-unknown-code.json", Caller.CUSTOMER, 404,
						"DISCOUNT_CODE_NOT_FOUND"),
				Arguments.of("inactive code", "request-inactive-code.json", Caller.CUSTOMER, 422,
						"DISCOUNT_CODE_INACTIVE"),
				Arguments.of("code not valid yet", "request-future-code.json", Caller.CUSTOMER, 422,
						"DISCOUNT_CODE_NOT_YET_VALID"),
				Arguments.of("expired code", "request-expired-code.json", Caller.CUSTOMER, 422,
						"DISCOUNT_CODE_EXPIRED"),
				Arguments.of("unknown product", "request-unknown-product.json", Caller.CUSTOMER, 404,
						"PRODUCT_NOT_FOUND"),
				Arguments.of("invalid quantity", "request-invalid-quantity.json", Caller.CUSTOMER, 400,
						"VALIDATION_ERROR"),
				Arguments.of("duplicated product", "request-duplicated-product.json", Caller.CUSTOMER, 400,
						"VALIDATION_ERROR"),
				Arguments.of("no token", "request-summer10.json", Caller.ANONYMOUS, 401, "UNAUTHORIZED"), Arguments.of(
						"token without CUSTOMER role", "request-summer10.json", Caller.WITHOUT_ROLE, 403, "FORBIDDEN"));
	}

	@ParameterizedTest(name = "{0} → {3} {4}")
	@MethodSource("rejectedRequests")
	void shouldRejectRequest_whenScenarioIsInvalid(String scenario, String fixture, Caller caller, int expectedStatus,
			String expectedCode) {
		// given
		String request = JsonFixtures.read("apply-discount/" + fixture);

		// when
		Response response = as(caller).body(request).post(APPLY_URL);

		// then
		assertSoftly(softly -> {
			softly.assertThat(response.statusCode()).as(scenario).isEqualTo(expectedStatus);
			softly.assertThat(response.jsonPath().getInt("status")).isEqualTo(expectedStatus);
			softly.assertThat(response.jsonPath().getString("code")).isEqualTo(expectedCode);
			softly.assertThat(response.jsonPath().getString("message")).isNotBlank();
		});
	}

	@Test
	void shouldExposeHealthWithoutToken_whenCallingActuator() {
		// given
		var healthUrl = "/actuator/health";

		// when
		Response response = given().get(healthUrl);

		// then
		assertSoftly(softly -> {
			softly.assertThat(response.statusCode()).isEqualTo(200);
			softly.assertThat(response.jsonPath().getString("status")).isEqualTo("UP");
		});
	}

	private RequestSpecification as(Caller caller) {
		var request = given().contentType(ContentType.JSON);
		return switch (caller) {
			case CUSTOMER -> request.auth().oauth2(keycloak.accessToken("customer", "customer"));
			case WITHOUT_ROLE -> request.auth().oauth2(keycloak.accessToken("viewer", "viewer"));
			case ANONYMOUS -> request;
		};
	}

	enum Caller {
		CUSTOMER, WITHOUT_ROLE, ANONYMOUS
	}
}
