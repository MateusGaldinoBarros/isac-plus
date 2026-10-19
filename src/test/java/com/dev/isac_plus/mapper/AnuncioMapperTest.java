package com.dev.isac_plus.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.dev.isac_plus.dto.AnuncioRequest;
import com.dev.isac_plus.dto.AnuncioResponse;
import com.dev.isac_plus.model.Anuncio;

class AnuncioMapperTest {

	private final AnuncioMapper mapper = new AnuncioMapper();

	@Test
	void toEntityCopiaOsCamposDoRequest() {
		Anuncio anuncio = mapper.toEntity(new AnuncioRequest("Bike", "Usada", "11999990000"));

		assertThat(anuncio.getId()).isNull();
		assertThat(anuncio.getTitulo()).isEqualTo("Bike");
		assertThat(anuncio.getDescricao()).isEqualTo("Usada");
		assertThat(anuncio.getContato()).isEqualTo("11999990000");
	}

	@Test
	void toResponseCopiaOsCamposDaEntidade() {
		AnuncioResponse response = mapper.toResponse(new Anuncio("Bike", "Usada", "11999990000"));

		assertThat(response).isEqualTo(new AnuncioResponse(null, "Bike", "Usada", "11999990000"));
	}

	@Test
	void atualizarEntitySobrescreveOsCampos() {
		Anuncio anuncio = new Anuncio("Bike", "Usada", "11999990000");

		mapper.atualizarEntity(anuncio, new AnuncioRequest("Moto", "Nova", "11777770000"));

		assertThat(mapper.toResponse(anuncio)).isEqualTo(new AnuncioResponse(null, "Moto", "Nova", "11777770000"));
	}
}
