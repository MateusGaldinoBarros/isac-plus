package com.dev.isac_plus.useCase;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dev.isac_plus.dto.EvolutionWebhookEvento;
import com.dev.isac_plus.service.AnuncioService;
import com.dev.isac_plus.service.WebhookService;

@Service
public class CriarAnuncioUseCase {

	private final WebhookService webhookService;
	private final AnuncioService anuncioService;

	public CriarAnuncioUseCase(WebhookService webhookService, AnuncioService anuncioService) {
		this.webhookService = webhookService;
		this.anuncioService = anuncioService;
	}

	@Transactional
	public void executar(EvolutionWebhookEvento evento) {
		webhookService.processar(evento).ifPresent(anuncioService::criarDeMensagem);
	}
}
