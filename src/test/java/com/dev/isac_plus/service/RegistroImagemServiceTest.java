package com.dev.isac_plus.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import com.dev.isac_plus.dto.ImagemRecebida;
import com.dev.isac_plus.dto.MensagemRecebida;
import com.dev.isac_plus.mapper.AnuncioMapper;
import com.dev.isac_plus.model.Anuncio;
import com.dev.isac_plus.model.ImagemAnuncio;
import com.dev.isac_plus.model.MetadadosImagem;
import com.dev.isac_plus.model.StatusImagem;
import com.dev.isac_plus.repository.AnuncioRepository;
import com.dev.isac_plus.repository.ImagemAnuncioRepository;
import com.dev.isac_plus.support.RelogioDeTeste;

import jakarta.persistence.EntityManager;

/**
 * Cenarios de agrupamento de fotos e texto em anuncios, com banco H2.
 * Cada teste simula a sequencia de mensagens que uma pessoa manda.
 */
@DataJpaTest
@Import({ RegistroImagemService.class, AnuncioService.class, AnuncioMapper.class, ValidadorImagem.class,
		RegistroImagemServiceTest.Relogio.class })
class RegistroImagemServiceTest {

	private static final String NUMERO = "5511999990000";

	@TestConfiguration
	static class Relogio {
		@Bean
		RelogioDeTeste relogio() {
			return new RelogioDeTeste();
		}
	}

	@Autowired
	private RegistroImagemService registro;

	@Autowired
	private AnuncioService anuncioService;

	@Autowired
	private AnuncioRepository anuncios;

	@Autowired
	private ImagemAnuncioRepository imagens;

	@Autowired
	private RelogioDeTeste relogio;

	@Autowired
	private EntityManager em;

	private int sequencia;

	@BeforeEach
	void zerarRelogio() {
		// O relogio e o mesmo bean em todos os testes da classe
		relogio.zerar();
	}

	// ---------- caminho feliz ----------

	@Test
	void textoEDepoisFotosSoltasFicamNoMesmoAnuncio() {
		texto("Vendo bicicleta aro 29");
		foto(null, null);
		foto(null, null);

		Anuncio anuncio = unicoAnuncio();
		assertThat(anuncio.getDescricao()).isEqualTo("Vendo bicicleta aro 29");
		assertThat(anuncio.isRascunho()).isFalse();
		assertThat(fotosDe(anuncio)).hasSize(2).extracting(ImagemAnuncio::getOrdem).containsExactly(0, 1);
	}

	@Test
	void textoEDepoisUmAlbumFicamNoMesmoAnuncio() {
		texto("Vendo bicicleta");
		foto(null, "ALBUM1");
		foto(null, "ALBUM1");
		foto(null, "ALBUM1");

		assertThat(fotosDe(unicoAnuncio())).hasSize(3);
	}

	@Test
	void fotoComLegendaCriaOAnuncioComADescricao() {
		foto("Vendo moto 150cc", null);

		Anuncio anuncio = unicoAnuncio();
		assertThat(anuncio.getDescricao()).isEqualTo("Vendo moto 150cc");
		assertThat(anuncio.isRascunho()).isFalse();
		assertThat(fotosDe(anuncio)).hasSize(1);
	}

	@Test
	void legendaEmQualquerFotoDoAlbumPreencheORascunho() {
		foto(null, "ALBUM1");
		foto("Vendo geladeira", "ALBUM1");
		foto(null, "ALBUM1");

		Anuncio anuncio = unicoAnuncio();
		assertThat(anuncio.getDescricao()).isEqualTo("Vendo geladeira");
		assertThat(anuncio.isRascunho()).isFalse();
		assertThat(fotosDe(anuncio)).hasSize(3);
	}

	// ---------- regra 1: fotos primeiro, texto depois ----------

	@Test
	void fotosPrimeiroViramRascunhoQueOTextoCompleta() {
		foto(null, null);
		foto(null, null);
		assertThat(unicoAnuncio().isRascunho()).isTrue();
		assertThat(anuncioService.listar()).as("rascunho fora da listagem").isEmpty();

		texto("Vendo bicicleta");

		Anuncio anuncio = unicoAnuncio();
		assertThat(anuncio.isRascunho()).isFalse();
		assertThat(anuncio.getDescricao()).isEqualTo("Vendo bicicleta");
		assertThat(fotosDe(anuncio)).hasSize(2);
		assertThat(anuncioService.listar()).hasSize(1);
	}

	@Test
	void textoDepoisDaJanelaNaoCompletaORascunhoAntigo() {
		foto(null, null);
		relogio.adiantar(Duration.ofMinutes(11));

		texto("Vendo bicicleta");

		assertThat(anuncios.findAll()).hasSize(2);
	}

	// ---------- regra 2: dois albuns sem legenda ----------

	@Test
	void doisAlbunsSemLegendaViramDoisAnuncios() {
		foto(null, "ALBUM_BIKE");
		foto(null, "ALBUM_BIKE");
		foto(null, "ALBUM_MOTO");
		foto(null, "ALBUM_MOTO");

		List<Anuncio> todos = anuncios.findAll();
		assertThat(todos).hasSize(2).allMatch(Anuncio::isRascunho);
		assertThat(todos).allSatisfy(a -> assertThat(fotosDe(a)).hasSize(2));
	}

	@Test
	void albumNovoDepoisDeAnuncioComFotosViraOutroAnuncio() {
		texto("Vendo bicicleta");
		foto(null, "ALBUM_BIKE");
		foto(null, "ALBUM_MOTO");

		assertThat(anuncios.findAll()).hasSize(2);
	}

	@Test
	void segundoAnuncioComTextoRecebeAsFotosSeguintes() {
		texto("Vendo bicicleta");
		foto(null, null);
		texto("Vendo moto");
		foto(null, null);

		List<Anuncio> todos = anuncios.findAll();
		assertThat(todos).hasSize(2);
		assertThat(todos).allSatisfy(a -> assertThat(fotosDe(a)).hasSize(1));
	}

	@Test
	void fotoComLegendaSempreCriaAnuncioNovo() {
		texto("Vendo bicicleta");
		foto("Vendo moto", null);

		assertThat(anuncios.findAll()).extracting(Anuncio::getDescricao)
				.containsExactlyInAnyOrder("Vendo bicicleta", "Vendo moto");
	}

	// ---------- janela de 10 minutos ----------

	@Test
	void fotoSoltaDepoisDaJanelaViraRascunhoNovo() {
		texto("Vendo bicicleta");
		relogio.adiantar(Duration.ofMinutes(11));
		foto(null, null);

		assertThat(anuncios.findAll()).hasSize(2);
		assertThat(anuncios.findAll()).filteredOn(Anuncio::isRascunho).hasSize(1);
	}

	@Test
	void numerosDiferentesNuncaSeMisturam() {
		texto("Vendo bicicleta");
		foto(null, null, "5521888880000");

		assertThat(anuncios.findAll()).hasSize(2);
	}

	// ---------- validacao e reenvio ----------

	@Test
	void reenvioDoMesmoWebhookNaoDuplica() {
		ImagemRecebida img = recebida("REPETIDA", null, null, NUMERO, "image/jpeg");
		registro.registrar(img);
		assertThat(registro.registrar(img)).isEmpty();

		assertThat(imagens.count()).isEqualTo(1);
	}

	@Test
	void tipoNaoAceitoFicaRejeitadoComOMotivo() {
		ImagemAnuncio img = registro.registrar(recebida("GIF", null, null, NUMERO, "image/gif")).orElseThrow();

		assertThat(img.getStatus()).isEqualTo(StatusImagem.REJEITADA);
		assertThat(img.getErro()).contains("image/gif");
	}

	@Test
	void decimaPrimeiraFotoDoAnuncioEhRejeitada() {
		texto("Vendo bicicleta");
		for (int i = 0; i < 11; i++) {
			foto(null, null);
		}

		List<ImagemAnuncio> todas = fotosDe(unicoAnuncio());
		assertThat(todas).filteredOn(i -> i.getStatus() == StatusImagem.PENDENTE).hasSize(10);
		assertThat(todas).filteredOn(i -> i.getStatus() == StatusImagem.REJEITADA).hasSize(1)
				.allSatisfy(i -> assertThat(i.getErro()).contains("maximo"));
	}

	// ---------- ajudantes ----------

	private void texto(String texto) {
		anuncioService.criarDeMensagem(new MensagemRecebida("TXT" + (++sequencia), "ong", NUMERO, "Ana", texto));
		em.flush();
	}

	private void foto(String legenda, String album) {
		foto(legenda, album, NUMERO);
	}

	private void foto(String legenda, String album, String numero) {
		registro.registrar(recebida("IMG" + (++sequencia), legenda, album, numero, "image/jpeg"));
		em.flush();
	}

	private ImagemRecebida recebida(String id, String legenda, String album, String numero, String tipo) {
		return new ImagemRecebida(id, "ong", numero, legenda, album, new MetadadosImagem(tipo, 80_000L, 1080, 1350),
				"{\"key\":{\"id\":\"" + id + "\"}}");
	}

	private Anuncio unicoAnuncio() {
		List<Anuncio> todos = anuncios.findAll();
		assertThat(todos).hasSize(1);
		return todos.get(0);
	}

	private List<ImagemAnuncio> fotosDe(Anuncio anuncio) {
		return imagens.findAll().stream()
				.filter(i -> i.getAnuncio().getId().equals(anuncio.getId()))
				.sorted((a, b) -> Integer.compare(a.getOrdem(), b.getOrdem()))
				.toList();
	}
}
