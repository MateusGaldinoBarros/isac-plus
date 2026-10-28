package com.dev.isac_plus.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import com.dev.isac_plus.exception.ImagemInvalidaException;

class ValidadorImagemTest {

	private static final long MB = 1024 * 1024;

	private final ValidadorImagem validador = new ValidadorImagem();

	@ParameterizedTest
	@ValueSource(strings = { "image/jpeg", "image/png", "image/webp", "IMAGE/JPEG", " image/png; charset=binary " })
	void aceitaTiposPermitidos(String tipo) {
		assertThat(validador.validarMetadados(tipo, 500_000L)).isEmpty();
	}

	@ParameterizedTest
	@ValueSource(strings = { "image/gif", "image/heic", "video/mp4", "application/pdf" })
	void rejeitaOutrosTipos(String tipo) {
		assertThat(validador.validarMetadados(tipo, 1000L)).hasValueSatisfying(m -> assertThat(m).contains("nao aceito"));
	}

	@Test
	void rejeitaTipoAusente() {
		assertThat(validador.validarMetadados(null, 1000L)).isPresent();
		assertThat(validador.validarMetadados("  ", 1000L)).isPresent();
	}

	@Test
	void limiteDeTamanhoEhDe10MB() {
		assertThat(validador.validarMetadados("image/jpeg", 10 * MB)).isEmpty();
		assertThat(validador.validarMetadados("image/jpeg", 10 * MB + 1))
				.hasValueSatisfying(m -> assertThat(m).contains("10 MB"));
	}

	@Test
	void tamanhoNaoInformadoPassaNosMetadados() {
		assertThat(validador.validarMetadados("image/jpeg", null)).isEmpty();
	}

	@ParameterizedTest
	@CsvSource({
			"FFD8FFE0,                 image/jpeg",
			"89504E470D0A1A0A0000,     image/png",
			"524946460000000057454250, image/webp" })
	void reconheceOTipoPeloConteudo(String hex, String esperado) {
		assertThat(validador.validarConteudo(bytes(hex))).isEqualTo(esperado);
	}

	@Test
	void conteudoQueNaoEhImagemEhRejeitadoMesmoComMimetypeDeImagem() {
		// Ex.: um PDF enviado como documento com mimetype image/jpeg
		assertThatThrownBy(() -> validador.validarConteudo("%PDF-1.7".getBytes()))
				.isInstanceOf(ImagemInvalidaException.class);
	}

	@Test
	void riffQueNaoEhWebpEhRejeitado() {
		// RIFF tambem e usado por WAV e AVI
		assertThatThrownBy(() -> validador.validarConteudo(bytes("524946460000000057415645")))
				.isInstanceOf(ImagemInvalidaException.class);
	}

	@Test
	void conteudoVazioOuCurtoEhRejeitado() {
		assertThatThrownBy(() -> validador.validarConteudo(new byte[0])).hasMessageContaining("vazio");
		assertThatThrownBy(() -> validador.validarConteudo(null)).isInstanceOf(ImagemInvalidaException.class);
		assertThatThrownBy(() -> validador.validarConteudo(bytes("FFD8"))).isInstanceOf(ImagemInvalidaException.class);
	}

	@Test
	void conteudoAcimaDe10MBEhRejeitado() {
		byte[] grande = new byte[(int) (10 * MB) + 1];
		grande[0] = (byte) 0xFF;
		grande[1] = (byte) 0xD8;
		grande[2] = (byte) 0xFF;

		assertThatThrownBy(() -> validador.validarConteudo(grande)).hasMessageContaining("10 MB");
	}

	@Test
	void limiteDe10ImagensPorAnuncio() {
		assertThat(validador.validarQuantidade(9)).isEmpty();
		assertThat(validador.validarQuantidade(10)).isPresent();
	}

	@Test
	void normalizarTipo() {
		assertThat(ValidadorImagem.normalizarTipo("Image/JPEG; x=y")).isEqualTo("image/jpeg");
		assertThat(ValidadorImagem.normalizarTipo("")).isNull();
		assertThat(ValidadorImagem.normalizarTipo(null)).isNull();
	}

	private static byte[] bytes(String hex) {
		byte[] resultado = new byte[hex.length() / 2];
		for (int i = 0; i < resultado.length; i++) {
			resultado[i] = (byte) Integer.parseInt(hex.substring(i * 2, i * 2 + 2), 16);
		}
		return resultado;
	}
}
