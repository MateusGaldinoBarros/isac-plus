package com.dev.isac_plus.useCase;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.dev.isac_plus.dto.EvolutionWebhookEvento;
import com.dev.isac_plus.dto.MensagemRecebida;
import com.dev.isac_plus.service.AnuncioService;
import com.dev.isac_plus.service.WebhookService;
import tools.jackson.databind.json.JsonMapper;

@ExtendWith(MockitoExtension.class)
class CriarAnuncioUseCaseTest {

	@Mock
	private WebhookService webhookService;

	@Mock
	private AnuncioService anuncioService;

	@Test
	void criaAnuncioQuandoWebhookRetornaMensagemValida() {
		EvolutionWebhookEvento evento = new EvolutionWebhookEvento("messages.upsert", "ong",
				JsonMapper.builder().build().createObjectNode());
		MensagemRecebida mensagem = new MensagemRecebida("ABC", "ong", "5511999", "Ana", "Vendo bicicleta");
		when(webhookService.processar(evento)).thenReturn(Optional.of(mensagem));
		CriarAnuncioUseCase useCase = new CriarAnuncioUseCase(webhookService, anuncioService);

		useCase.executar(evento);

		verify(anuncioService).criarDeMensagem(mensagem);
	}

	@Test
	void naoCriaAnuncioQuandoWebhookIgnoraEvento() {
		EvolutionWebhookEvento evento = new EvolutionWebhookEvento("contacts.upsert", "ong",
				JsonMapper.builder().build().createObjectNode());
		when(webhookService.processar(evento)).thenReturn(Optional.empty());
		CriarAnuncioUseCase useCase = new CriarAnuncioUseCase(webhookService, anuncioService);

		useCase.executar(evento);

		verify(anuncioService, never()).criarDeMensagem(org.mockito.ArgumentMatchers.any());
	}
}
