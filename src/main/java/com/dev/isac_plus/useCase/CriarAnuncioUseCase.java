package com.dev.isac_plus.useCase;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionOperations;

import com.dev.isac_plus.dto.EvolutionWebhookEvento;
import com.dev.isac_plus.dto.ImagemRecebida;
import com.dev.isac_plus.dto.MensagemRecebida;
import com.dev.isac_plus.service.AnuncioService;
import com.dev.isac_plus.service.RegistroImagemService;
import com.dev.isac_plus.service.TravaPorNumero;
import com.dev.isac_plus.service.WebhookService;

@Service
public class CriarAnuncioUseCase {

	private final WebhookService webhookService;
	private final AnuncioService anuncioService;
	private final RegistroImagemService registroImagemService;
	private final TravaPorNumero trava;
	private final TransactionOperations transacao;

	public CriarAnuncioUseCase(WebhookService webhookService, AnuncioService anuncioService,
			RegistroImagemService registroImagemService, TravaPorNumero trava, TransactionOperations transacao) {
		this.webhookService = webhookService;
		this.anuncioService = anuncioService;
		this.registroImagemService = registroImagemService;
		this.trava = trava;
		this.transacao = transacao;
	}

	/**
	 * A transacao fica dentro da trava: a proxima mensagem do mesmo numero so comeca depois que esta
	 * foi gravada, entao ela enxerga o anuncio ou rascunho que acabou de ser criado.
	 */
	public void executar(EvolutionWebhookEvento evento) {
		String numero = webhookService.numeroDoRemetente(evento).orElse("");
		trava.executar(numero, () -> transacao.executeWithoutResult(status -> processar(evento)));
	}

	private void processar(EvolutionWebhookEvento evento) {
		Optional<MensagemRecebida> mensagem = webhookService.processar(evento);
		Optional<ImagemRecebida> imagem = webhookService.extrairImagem(evento);
		if (imagem.isPresent()) {
			// A legenda, se houver, e tratada junto com a imagem
			registroImagemService.registrar(imagem.get());
		} else {
			mensagem.ifPresent(anuncioService::criarDeMensagem);
		}
	}
}
