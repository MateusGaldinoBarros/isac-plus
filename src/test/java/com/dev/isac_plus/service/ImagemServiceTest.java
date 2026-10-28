package com.dev.isac_plus.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.time.Clock;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.dev.isac_plus.dto.AnuncioResponse;
import com.dev.isac_plus.dto.ArquivoImagem;
import com.dev.isac_plus.dto.ImagemResponse;
import com.dev.isac_plus.dto.MensagemRecebida;
import com.dev.isac_plus.exception.RecursoNaoEncontradoException;
import com.dev.isac_plus.mapper.AnuncioMapper;
import com.dev.isac_plus.model.Anuncio;
import com.dev.isac_plus.model.ImagemAnuncio;
import com.dev.isac_plus.model.MetadadosImagem;
import com.dev.isac_plus.repository.AnuncioRepository;
import com.dev.isac_plus.repository.ImagemAnuncioRepository;

import jakarta.persistence.EntityManager;

/** O que o front enxerga: GET /imagens/{id} e a lista de imagens no anuncio. Banco H2, storage simulado. */
@DataJpaTest
@Import({ ImagemService.class, AnuncioService.class, AnuncioMapper.class, ImagemServiceTest.Config.class })
class ImagemServiceTest {

	private static final byte[] JPEG = { (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x01 };

	@TestConfiguration
	static class Config {
		@Bean
		Clock relogio() {
			return Clock.systemUTC();
		}
	}

	@MockitoBean
	private StorageService storage;

	@Autowired
	private ImagemService imagemService;

	@Autowired
	private AnuncioService anuncioService;

	@Autowired
	private AnuncioRepository anuncios;

	@Autowired
	private ImagemAnuncioRepository imagens;

	@Autowired
	private EntityManager em;

	private Anuncio publicado;

	@BeforeEach
	void setUp() {
		publicado = anuncios.save(new Anuncio("", "Vendo bike", "5511999"));
	}

	@Test
	void entregaImagemSalvaDeAnuncioPublicado() {
		ImagemAnuncio salva = salva(publicado, "A", 0, "anuncios/1/a.jpg");
		when(storage.baixar("anuncios/1/a.jpg")).thenReturn(JPEG);

		ArquivoImagem arquivo = imagemService.buscarArquivo(salva.getId());

		assertThat(arquivo.conteudo()).isEqualTo(JPEG);
		assertThat(arquivo.contentType()).isEqualTo("image/jpeg");
	}

	@Test
	void imagemPendenteOuRejeitadaResponde404() {
		ImagemAnuncio pendente = imagens.save(nova(publicado, "P", 0));
		ImagemAnuncio rejeitada = nova(publicado, "R", 1);
		rejeitada.rejeitar("gif");
		imagens.save(rejeitada);

		assertThatThrownBy(() -> imagemService.buscarArquivo(pendente.getId()))
				.isInstanceOf(RecursoNaoEncontradoException.class);
		assertThatThrownBy(() -> imagemService.buscarArquivo(rejeitada.getId()))
				.isInstanceOf(RecursoNaoEncontradoException.class);
	}

	@Test
	void imagemDeRascunhoResponde404() {
		Anuncio rascunho = anuncios.save(Anuncio.rascunho("5511999"));
		ImagemAnuncio salva = salva(rascunho, "A", 0, "anuncios/2/a.jpg");

		assertThatThrownBy(() -> imagemService.buscarArquivo(salva.getId()))
				.isInstanceOf(RecursoNaoEncontradoException.class);
	}

	@Test
	void anuncioTrazSoAsImagensSalvasNaOrdemComAUrl() {
		ImagemAnuncio segunda = salva(publicado, "B", 1, "anuncios/1/b.jpg");
		ImagemAnuncio primeira = salva(publicado, "A", 0, "anuncios/1/a.jpg");
		imagens.save(nova(publicado, "PENDENTE", 2));
		em.flush();
		em.clear();

		AnuncioResponse resposta = anuncioService.buscarPorId(publicado.getId());

		assertThat(resposta.imagens()).extracting(ImagemResponse::id).containsExactly(primeira.getId(), segunda.getId());
		assertThat(resposta.imagens().get(0)).isEqualTo(
				new ImagemResponse(primeira.getId(), "/imagens/" + primeira.getId(), 1080, 1350, 0));
	}

	@Test
	void listagemTrazAsImagensDeCadaAnuncio() {
		Anuncio outro = anuncios.save(new Anuncio("", "Vendo moto", "5511888"));
		salva(publicado, "A", 0, "k1");
		salva(outro, "B", 0, "k2");
		salva(outro, "C", 1, "k3");
		em.flush();
		em.clear();

		assertThat(anuncioService.listar()).extracting(r -> r.imagens().size()).containsExactlyInAnyOrder(1, 2);
	}

	@Test
	void anuncioSemImagensTrazListaVazia() {
		assertThat(anuncioService.buscarPorId(publicado.getId()).imagens()).isEmpty();
	}

	private ImagemAnuncio salva(Anuncio anuncio, String id, int ordem, String chave) {
		ImagemAnuncio imagem = nova(anuncio, id, ordem);
		imagem.marcarSalva(chave, new MetadadosImagem("image/jpeg", 4L, null, null));
		return imagens.save(imagem);
	}

	private ImagemAnuncio nova(Anuncio anuncio, String id, int ordem) {
		return new ImagemAnuncio(anuncio, id, null, "ong", anuncio.getContato(),
				new MetadadosImagem("image/jpeg", 80_000L, 1080, 1350), ordem, "{}");
	}
}
