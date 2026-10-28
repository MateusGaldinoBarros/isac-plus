package com.dev.isac_plus.useCase;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionOperations;

import com.dev.isac_plus.dto.EvolutionWebhookEvento;
import com.dev.isac_plus.dto.ImagemRecebida;
import com.dev.isac_plus.dto.MensagemRecebida;
import com.dev.isac_plus.model.MetadadosImagem;
import com.dev.isac_plus.service.AnuncioService;
import com.dev.isac_plus.service.RegistroImagemService;
import com.dev.isac_plus.service.TravaPorNumero;
import com.dev.isac_plus.service.WebhookService;

import tools.jackson.databind.json.JsonMapper;

@ExtendWith(MockitoExtension.class)
class CriarAnuncioUseCaseTest {

	@Mock
	private WebhookService webhookService;

	@Mock
	private AnuncioService anuncioService;

	@Mock
	private RegistroImagemService registroImagemService;

	private CriarAnuncioUseCase useCase;

	private final EvolutionWebhookEvento evento = new EvolutionWebhookEvento("messages.upsert", "ong",
			JsonMapper.builder().build().createObjectNode());

	@BeforeEach
	void setUp() {
		useCase = new CriarAnuncioUseCase(webhookService, anuncioService, registroImagemService, new TravaPorNumero(),
				TransactionOperations.withoutTransaction());
	}

	@Test
	void criaAnuncioQuandoWebhookRetornaMensagemDeTexto() {
		MensagemRecebida mensagem = new MensagemRecebida("ABC", "ong", "5511999", "Ana", "Vendo bicicleta");
		when(webhookService.processar(evento)).thenReturn(Optional.of(mensagem));
		when(webhookService.extrairImagem(evento)).thenReturn(Optional.empty());

		useCase.executar(evento);

		verify(anuncioService).criarDeMensagem(mensagem);
		verify(registroImagemService, never()).registrar(any());
	}

	@Test
	void imagemComLegendaVaiParaORegistroDeImagemENaoCriaAnuncioDeTextoSeparado() {
		ImagemRecebida imagem = new ImagemRecebida("IMG", "ong", "5511999", "Vendo bike", null,
				new MetadadosImagem("image/jpeg", 1L, null, null), "{}");
		when(webhookService.processar(evento))
				.thenReturn(Optional.of(new MensagemRecebida("IMG", "ong", "5511999", "Ana", "Vendo bike")));
		when(webhookService.extrairImagem(evento)).thenReturn(Optional.of(imagem));

		useCase.executar(evento);

		verify(registroImagemService).registrar(imagem);
		verify(anuncioService, never()).criarDeMensagem(any());
	}

	@Test
	void naoFazNadaQuandoWebhookIgnoraEvento() {
		when(webhookService.processar(evento)).thenReturn(Optional.empty());
		when(webhookService.extrairImagem(evento)).thenReturn(Optional.empty());

		useCase.executar(evento);

		verify(anuncioService, never()).criarDeMensagem(any());
		verify(registroImagemService, never()).registrar(any());
	}
}
