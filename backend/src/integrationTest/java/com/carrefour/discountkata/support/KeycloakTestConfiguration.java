package com.carrefour.discountkata.support;

import dasniko.testcontainers.keycloak.KeycloakContainer;
import io.restassured.RestAssured;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.test.context.DynamicPropertyRegistrar;

/**
 * Real Keycloak started with Testcontainers and the same realm file as
 * docker-compose, so that the integration tests also prove the realm
 * configuration (CUSTOMER role, "roles" claim mapper, users).
 */
@TestConfiguration(proxyBeanMethods = false)
@Profile("it")
public class KeycloakTestConfiguration {

	private static final String REALM = "discount";
	private static final String CLIENT_ID = "discount-cli";

	private final KeycloakContainer keycloak = new KeycloakContainer("quay.io/keycloak/keycloak:26.8.0")
			.withRealmImportFile("/discount-realm.json");

	@Bean
	public KeycloakContainer keycloak() {
		return keycloak;
	}

	@Bean
	public DynamicPropertyRegistrar issuerUriRegistrar() {
		return registry -> registry.add("spring.security.oauth2.resourceserver.jwt.issuer-uri", this::issuerUri);
	}

	/**
	 * Gets an access token with the password grant of the demo client, like the
	 * curl command of the README.
	 */
	public String accessToken(String username, String password) {
		return RestAssured.given().formParam("grant_type", "password").formParam("client_id", CLIENT_ID)
				.formParam("username", username).formParam("password", password)
				.post(issuerUri() + "/protocol/openid-connect/token").then().statusCode(200).extract()
				.path("access_token");
	}

	private String issuerUri() {
		return keycloak.getAuthServerUrl() + "/realms/" + REALM;
	}
}
