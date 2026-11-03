package com.carrefour.discountkata.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * PostgreSQL started with Testcontainers; Spring Boot connects to it through
 * {@link ServiceConnection}. Same image as docker-compose.
 */
@TestConfiguration(proxyBeanMethods = false)
@Profile("it")
public class PostgresTestConfiguration {

	@Bean
	@ServiceConnection
	public PostgreSQLContainer postgres() {
		return new PostgreSQLContainer(DockerImageName.parse("postgres:17-alpine"));
	}
}
