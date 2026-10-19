package com.dev.isac_plus.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

class AnuncioRequestTest {

	private static ValidatorFactory factory;
	private static Validator validator;

	@BeforeAll
	static void setUp() {
		factory = Validation.buildDefaultValidatorFactory();
		validator = factory.getValidator();
	}

	@AfterAll
	static void tearDown() {
		factory.close();
	}

	@Test
	void requestValidoNaoTemViolacoes() {
		assertThat(validator.validate(new AnuncioRequest("Bike", "Usada", "11999990000"))).isEmpty();
	}

	@Test
	void descricaoEhOpcional() {
		assertThat(validator.validate(new AnuncioRequest("Bike", null, "11999990000"))).isEmpty();
	}

	@Test
	void tituloEContatoSaoObrigatorios() {
		Set<ConstraintViolation<AnuncioRequest>> violacoes = validator.validate(new AnuncioRequest(" ", "Usada", null));

		assertThat(violacoes).extracting(v -> v.getPropertyPath().toString())
				.containsExactlyInAnyOrder("titulo", "contato");
	}

	@Test
	void tituloMuitoLongoEhInvalido() {
		Set<ConstraintViolation<AnuncioRequest>> violacoes = validator.validate(new AnuncioRequest("a".repeat(121), null, "11999990000"));

		assertThat(violacoes).extracting(v -> v.getPropertyPath().toString()).containsExactly("titulo");
	}
}
