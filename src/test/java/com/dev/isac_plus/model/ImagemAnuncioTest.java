package com.dev.isac_plus.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.Test;

class ImagemAnuncioTest {

	private static final Instant AGORA = Instant.parse("2026-10-27T12:00:00Z");

	@Test
	void nasceComoPendenteProntaParaTentar() {
		Instant antes = Instant.now();
		ImagemAnuncio imagem = nova();

		assertThat(imagem.getStatus()).isEqualTo(StatusImagem.PENDENTE);
		assertThat(imagem.getTentativas()).isZero();
		assertThat(imagem.getProximaTentativaEm()).isBetween(antes, Instant.now());
		assertThat(imagem.getContentType()).isEqualTo("image/jpeg");
		assertThat(imagem.getLargura()).isEqualTo(1080);
		assertThat(imagem.getChaveStorage()).isNull();
	}

	@Test
	void marcarSalvaGuardaAChaveEOsDadosReais() {
		ImagemAnuncio imagem = nova();
		imagem.registrarFalha("timeout", AGORA);

		imagem.marcarSalva("anuncios/1/a.png", new MetadadosImagem("image/png", 2048L, null, 900));

		assertThat(imagem.getStatus()).isEqualTo(StatusImagem.SALVA);
		assertThat(imagem.getChaveStorage()).isEqualTo("anuncios/1/a.png");
		assertThat(imagem.getContentType()).isEqualTo("image/png");
		assertThat(imagem.getTamanhoBytes()).isEqualTo(2048L);
		assertThat(imagem.getLargura()).as("mantem a do webhook quando falta").isEqualTo(1080);
		assertThat(imagem.getAltura()).isEqualTo(900);
		assertThat(imagem.getProximaTentativaEm()).isNull();
		assertThat(imagem.getErro()).isNull();
	}

	@Test
	void rejeitarGuardaOMotivoESaiDaFila() {
		ImagemAnuncio imagem = nova();

		imagem.rejeitar("Tipo image/gif nao aceito");

		assertThat(imagem.getStatus()).isEqualTo(StatusImagem.REJEITADA);
		assertThat(imagem.getErro()).isEqualTo("Tipo image/gif nao aceito");
		assertThat(imagem.getProximaTentativaEm()).isNull();
	}

	@Test
	void falhasAgendamNovasTentativasComEsperaCrescenteAteDesistir() {
		ImagemAnuncio imagem = nova();

		imagem.registrarFalha("erro 1", AGORA);
		assertThat(imagem.getProximaTentativaEm()).isEqualTo(AGORA.plus(Duration.ofSeconds(30)));

		imagem.registrarFalha("erro 2", AGORA);
		assertThat(imagem.getProximaTentativaEm()).isEqualTo(AGORA.plus(Duration.ofMinutes(2)));

		imagem.registrarFalha("erro 3", AGORA);
		assertThat(imagem.getProximaTentativaEm()).isEqualTo(AGORA.plus(Duration.ofMinutes(10)));
		assertThat(imagem.getStatus()).isEqualTo(StatusImagem.PENDENTE);

		imagem.registrarFalha("erro 4", AGORA);
		assertThat(imagem.getStatus()).isEqualTo(StatusImagem.FALHOU);
		assertThat(imagem.getProximaTentativaEm()).isNull();
		assertThat(imagem.getTentativas()).isEqualTo(4);
		assertThat(imagem.getErro()).isEqualTo("erro 4");
	}

	@Test
	void naoMudaDeStatusDepoisDeFinalizada() {
		ImagemAnuncio salva = nova();
		salva.marcarSalva("k", new MetadadosImagem("image/jpeg", 1L, null, null));

		assertThatThrownBy(() -> salva.registrarFalha("x", AGORA)).isInstanceOf(IllegalStateException.class);
		assertThatThrownBy(() -> salva.rejeitar("x")).isInstanceOf(IllegalStateException.class);
		assertThatThrownBy(() -> salva.marcarSalva("k2", new MetadadosImagem("image/jpeg", 1L, null, null)))
				.isInstanceOf(IllegalStateException.class);
	}

	@Test
	void anuncioExpoeImagensSomenteLeitura() {
		Anuncio anuncio = new Anuncio("", "Bike", "5511999");

		assertThat(anuncio.getImagens()).isEmpty();
		assertThatThrownBy(() -> anuncio.getImagens().add(nova())).isInstanceOf(UnsupportedOperationException.class);
	}

	static ImagemAnuncio nova() {
		return new ImagemAnuncio(new Anuncio("", "Bike", "5511999"), "MSG1", null, "ong", "5511999",
				new MetadadosImagem("image/jpeg", 84512L, 1080, 1350), 0, "{\"key\":{\"id\":\"MSG1\"}}");
	}
}
