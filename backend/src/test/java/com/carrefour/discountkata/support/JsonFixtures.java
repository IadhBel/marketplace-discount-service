package com.carrefour.discountkata.support;

import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

public final class JsonFixtures {

	private JsonFixtures() {
	}

	public static String read(String path) {
		try {
			return new ClassPathResource("json/" + path).getContentAsString(StandardCharsets.UTF_8);
		} catch (IOException exception) {
			throw new UncheckedIOException("Cannot read JSON fixture " + path, exception);
		}
	}
}
