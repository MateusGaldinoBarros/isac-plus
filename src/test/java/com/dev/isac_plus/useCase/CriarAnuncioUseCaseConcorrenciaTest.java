package com.dev.isac_plus.useCase;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import com.dev.isac_plus.dto.EvolutionWebhookEvento;
import com.dev.isac_plus.mapper.AnuncioMapper;
import com.dev.isac_plus.repository.AnuncioRepository;
import com.dev.isac_plus.repository.ImagemAnuncioRepository;
import com.dev.isac_plus.repository.MensagemRepository;
import com.dev.isac_plus.service.AnuncioService;
import com.dev.isac_plus.service.RegistroImagemService;
import com.dev.isac_plus.service.TravaPorNumero;
import com.dev.isac_plus.service.ValidadorImagem;
import com.dev.isac_plus.service.WebhookService;
import com.dev.isac_plus.support.RelogioDeTeste;

import tools.jackson.databind.json.JsonMapper;

/**
 * Fotos de um album chegam em webhooks paralelos. Aqui elas chegam todas ao mesmo tempo, cada uma
 * na sua thread e com a sua transacao de verdade (sem a transacao de teste), e tem que virar um anuncio so.
 */
@DataJpaTest
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Import({ CriarAnuncioUseCase.class, WebhookService.class, AnuncioService.class, AnuncioMapper.class,
		RegistroImagemService.class, ValidadorImagem.class, TravaPorNumero.class,
		CriarAnuncioUseCaseConcorrenciaTest.Config.class })
class CriarAnuncioUseCaseConcorrenciaTest {

	private static final int FOTOS = 10;

	@TestConfiguration
	static class Config {
		@Bean
		RelogioDeTeste relogio() {
			return new RelogioDeTeste();
		}

		@Bean
		TransactionTemplate transactionTemplate(PlatformTransactionManager gerenciador) {
			return new TransactionTemplate(gerenciador);
		}
	}

	@Autowired
	private CriarAnuncioUseCase useCase;

	@Autowired
	private AnuncioRepository anuncios;

	@Autowired
	private ImagemAnuncioRepository imagens;

	@Autowired
	private MensagemRepository mensagens;

	private final JsonMapper json = JsonMapper.builder().build();

	@AfterEach
	void limpar() {
		imagens.deleteAll();
		anuncios.deleteAll();
		mensagens.deleteAll();
	}

	@Test
	void fotosDoMesmoAlbumChegandoJuntasViramUmAnuncioSo() throws Exception {
		ExecutorService threads = Executors.newFixedThreadPool(FOTOS);
		CountDownLatch largada = new CountDownLatch(1);
		List<Future<?>> envios = new ArrayList<>();
		for (int i = 0; i < FOTOS; i++) {
			EvolutionWebhookEvento evento = fotoDoAlbum("FOTO" + i, i == 3 ? "Vendo bicicleta" : null);
			envios.add(threads.submit(() -> {
				largada.await();
				useCase.executar(evento);
				return null;
			}));
		}

		largada.countDown();
		for (Future<?> envio : envios) {
			envio.get(30, TimeUnit.SECONDS);
		}
		threads.shutdown();

		assertThat(anuncios.findAll()).hasSize(1)
				.allSatisfy(a -> assertThat(a.getDescricao()).isEqualTo("Vendo bicicleta"));
		assertThat(imagens.count()).isEqualTo(FOTOS);
	}

	private EvolutionWebhookEvento fotoDoAlbum(String id, String legenda) {
		String caption = legenda == null ? "" : ",\"caption\":\"" + legenda + "\"";
		return new EvolutionWebhookEvento("messages.upsert", "ong", json.readTree("""
				{"key":{"remoteJid":"5511999990000@s.whatsapp.net","fromMe":false,"id":"%s"},
				 "message":{"imageMessage":{"mimetype":"image/jpeg","fileLength":"80000"%s},
				   "messageContextInfo":{"messageAssociation":{"parentMessageKey":{"id":"ALBUM1"}}}}}
				""".formatted(id, caption)));
	}
}
