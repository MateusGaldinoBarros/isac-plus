package com.dev.isac_plus.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AnuncioTest {

	@Test
	void construtorPreencheOsCampos() {
		Anuncio anuncio = new Anuncio("Bike", "Usada", "11999990000");

		assertThat(anuncio.getId()).isNull();
		assertThat(anuncio.getTitulo()).isEqualTo("Bike");
		assertThat(anuncio.getDescricao()).isEqualTo("Usada");
		assertThat(anuncio.getContato()).isEqualTo("11999990000");
	}

	@Test
	void settersAlteramOsCampos() {
		Anuncio anuncio = new Anuncio("Bike", "Usada", "11999990000");

		anuncio.setTitulo("Bike aro 29");
		anuncio.setDescricao("Seminova");
		anuncio.setContato("11888880000");

		assertThat(anuncio.getTitulo()).isEqualTo("Bike aro 29");
		assertThat(anuncio.getDescricao()).isEqualTo("Seminova");
		assertThat(anuncio.getContato()).isEqualTo("11888880000");
	}
}
