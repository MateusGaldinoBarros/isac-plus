package com.dev.isac_plus.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.dev.isac_plus.client.EvolutionClient;
import com.dev.isac_plus.dto.MidiaBaixada;
import com.dev.isac_plus.exception.EvolutionException;
import com.dev.isac_plus.exception.StorageException;
import com.dev.isac_plus.model.Anuncio;
import com.dev.isac_plus.model.ImagemAnuncio;
import com.dev.isac_plus.model.MetadadosImagem;
import com.dev.isac_plus.model.StatusImagem;
import com.dev.isac_plus.repository.AnuncioRepository;
import com.dev.isac_plus.repository.ImagemAnuncioRepository;
import com.dev.isac_plus.support.RelogioDeTeste;

import tools.jackson.databind.json.JsonMapper;

/**
 * Processamento das imagens pendentes com banco H2 de verdade e Evolution/storage simulados.
 * Sem a transacao de teste, como em producao: as entidades saem do banco ja desligadas da sessao.
 */
@DataJpaTest
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Import({ ProcessadorImagens.class, ValidadorImagem.class, ProcessadorImagensTest.Config.class })
class ProcessadorImagensTest {

	private static final byte[] JPEG = { (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x01, 0x02 };
	private static final byte[] PNG = { (byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n', 0x00 };

	@TestConfiguration
	static class Config {
		@Bean
		RelogioDeTeste relogio() {
			return new RelogioDeTeste();
		}

		@Bean
		JsonMapper jsonMapper() {
			return JsonMapper.builder().build();
		}
	}

	@MockitoBean
	private EvolutionClient evolution;

	@MockitoBean
	private StorageService storage;

	@Autowired
	private ImagemAnuncioRepository imagens;

	@Autowired
	private AnuncioRepository anuncios;

	@Autowired
	private ProcessadorImagens processador;

	@Autowired
	private RelogioDeTeste relogio;

	private Anuncio anuncio;

	@BeforeEach
	void setUp() {
		relogio.zerar();
		anuncio = anuncios.save(new Anuncio("", "Vendo bike", "5511999"));
	}

	@AfterEach
	void limpar() {
		imagens.deleteAll();
		anuncios.deleteAll();
	}

	@Test
	void baixaConfereGuardaEMarcaComoSalva() {
		Long id = pendente("IMG1").getId();
		when(evolution.baixarMidia(eq("ong"), any())).thenReturn(new MidiaBaixada(PNG, "image/jpeg", "x.jpg", 800, 600));

		processador.processarPendentes();

		ImagemAnuncio salva = imagens.findById(id).orElseThrow();
		assertThat(salva.getStatus()).isEqualTo(StatusImagem.SALVA);
		assertThat(salva.getChaveStorage()).matches("anuncios/" + anuncio.getId() + "/[0-9a-f-]{36}\\.png");
		assertThat(salva.getContentType()).as("tipo real pelos bytes, nao o informado").isEqualTo("image/png");
		assertThat(salva.getTamanhoBytes()).isEqualTo(PNG.length);
		assertThat(salva.getLargura()).isEqualTo(800);
		verify(storage).enviar(salva.getChaveStorage(), PNG, "image/png");
	}

	@Test
	void mandaParaAEvolutionOPayloadOriginalDoWebhook() {
		pendente("IMG1");
		when(evolution.baixarMidia(eq("ong"), any())).thenReturn(new MidiaBaixada(JPEG, null, null, null, null));

		processador.processarPendentes();

		verify(evolution).baixarMidia(eq("ong"),
				org.mockito.ArgumentMatchers.argThat(no -> "IMG1".equals(no.path("key").path("id").asString())));
	}

	@Test
	void arquivoQueNaoEhImagemFicaRejeitadoSemTentarDeNovo() {
		Long id = pendente("IMG1").getId();
		when(evolution.baixarMidia(anyString(), any()))
				.thenReturn(new MidiaBaixada("%PDF-1.7".getBytes(), "image/jpeg", null, null, null));

		processador.processarPendentes();

		ImagemAnuncio img = imagens.findById(id).orElseThrow();
		assertThat(img.getStatus()).isEqualTo(StatusImagem.REJEITADA);
		assertThat(img.getProximaTentativaEm()).isNull();
		verify(storage, never()).enviar(anyString(), any(), anyString());
	}

	@Test
	void evolutionForaDoArAgendaNovaTentativaEDesisteNaQuartaFalha() {
		Long id = pendente("IMG1").getId();
		when(evolution.baixarMidia(anyString(), any())).thenThrow(new EvolutionException("Connection refused"));

		processador.processarPendentes();
		ImagemAnuncio img = imagens.findById(id).orElseThrow();
		assertThat(img.getStatus()).isEqualTo(StatusImagem.PENDENTE);
		assertThat(img.getTentativas()).isEqualTo(1);
		assertThat(img.getErro()).contains("Connection refused");

		processador.processarPendentes();
		assertThat(imagens.findById(id).orElseThrow().getTentativas()).as("ainda nao chegou a hora").isEqualTo(1);

		for (Duration espera : List.of(Duration.ofSeconds(31), Duration.ofMinutes(3), Duration.ofMinutes(11))) {
			relogio.adiantar(espera);
			processador.processarPendentes();
		}

		img = imagens.findById(id).orElseThrow();
		assertThat(img.getStatus()).isEqualTo(StatusImagem.FALHOU);
		assertThat(img.getTentativas()).isEqualTo(4);
	}

	@Test
	void storageForaDoArContaComoFalhaPassageira() {
		Long id = pendente("IMG1").getId();
		when(evolution.baixarMidia(anyString(), any())).thenReturn(new MidiaBaixada(JPEG, null, null, null, null));
		doThrow(new StorageException("storage fora", null)).when(storage).enviar(anyString(), any(), anyString());

		processador.processarPendentes();

		ImagemAnuncio img = imagens.findById(id).orElseThrow();
		assertThat(img.getStatus()).isEqualTo(StatusImagem.PENDENTE);
		assertThat(img.getTentativas()).isEqualTo(1);
	}

	@Test
	void umaImagemComErroNaoAtrapalhaAsOutras() {
		Long ruim = pendente("RUIM").getId();
		Long boa = pendente("BOA").getId();
		when(evolution.baixarMidia(anyString(), any())).thenAnswer(inv -> {
			if ("RUIM".equals(inv.getArgument(1, tools.jackson.databind.JsonNode.class).path("key").path("id").asString())) {
				throw new EvolutionException("midia expirada");
			}
			return new MidiaBaixada(JPEG, null, null, null, null);
		});

		processador.processarPendentes();

		assertThat(imagens.findById(ruim).orElseThrow().getStatus()).isEqualTo(StatusImagem.PENDENTE);
		assertThat(imagens.findById(boa).orElseThrow().getStatus()).isEqualTo(StatusImagem.SALVA);
	}

	@Test
	void naoMexeEmImagensJaFinalizadas() {
		ImagemAnuncio rejeitada = new ImagemAnuncio(anuncio, "GIF", null, "ong", "5511999",
				new MetadadosImagem("image/gif", 1L, null, null), 0, "{}");
		rejeitada.rejeitar("gif");
		imagens.save(rejeitada);

		processador.processarPendentes();

		verify(evolution, never()).baixarMidia(anyString(), any());
		assertThat(imagens.findById(rejeitada.getId())).map(ImagemAnuncio::getStatus).isEqualTo(Optional.of(StatusImagem.REJEITADA));
	}

	private ImagemAnuncio pendente(String idWhatsapp) {
		return imagens.save(new ImagemAnuncio(anuncio, idWhatsapp, null, "ong", "5511999",
				new MetadadosImagem("image/jpeg", 80_000L, 1080, 1350), 0,
				"{\"key\":{\"id\":\"" + idWhatsapp + "\"},\"message\":{\"imageMessage\":{}}}"));
	}
}
