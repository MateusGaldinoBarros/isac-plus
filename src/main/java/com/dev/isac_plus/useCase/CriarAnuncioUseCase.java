package com.dev.isac_plus.useCase;

import java.text.Normalizer;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionOperations;

import com.dev.isac_plus.dto.EvolutionWebhookEvento;
import com.dev.isac_plus.dto.MensagemEnviadaDTO;
import com.dev.isac_plus.dto.MensagemRecebida;
import com.dev.isac_plus.restClient.EvolutionClient;
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
	private final EvolutionClient evolutionClient;
	private final ConcurrentHashMap<Contato, EtapaConversa> conversas = new ConcurrentHashMap<>();

	private static final String PERGUNTA_CONFIRMACAO = "Olá! 👋 Eu sou o Isac-plus, seu assistente para criação de anúncios.\n\nGostaria de criar um anúncio? Responda sim ou não para continuarmos.";
	private static final String PEDIDO_TITULO = "Que bom poder ajudar! 😊\n\nPor favor, informe o título que gostaria de usar no anúncio.";
	private static final String PEDIDO_TEXTO = "Obrigado pelo título!\n\nAgora, por gentileza, envie o texto com os detalhes que deseja publicar no anúncio.";
	private static final String ANUNCIO_CRIADO = "Anúncio criado com sucesso! 🎉\n\nAgradeço por utilizar o Isac-plus. Se precisar de ajuda com outro anúncio, é só me chamar.";
	private static final String ANUNCIO_CANCELADO = "Tudo bem, não criarei o anúncio. 😊\n\nQuando quiser começar, basta enviar uma nova mensagem. Estarei à disposição!";
	private static final String PEDIDO_TITULO_NOVAMENTE = "Não consegui identificar um título nessa mensagem.\n\nPor favor, envie o título que deseja usar no anúncio.";
	private static final String PEDIDO_TEXTO_NOVAMENTE = "Não consegui identificar o texto do anúncio nessa mensagem.\n\nPor favor, envie os detalhes que deseja publicar.";

	public CriarAnuncioUseCase(WebhookService webhookService, AnuncioService anuncioService, EvolutionClient evolutionClient) {
		this.webhookService = webhookService;
		this.anuncioService = anuncioService;
		this.evolutionClient = evolutionClient;
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
		webhookService.processar(evento).ifPresent(this::processarMensagem);
	}

	private void processarMensagem(MensagemRecebida mensagem) {
		Contato contato = new Contato(mensagem.instancia(), mensagem.numero());
		EtapaConversa etapa = conversas.get(contato);

		if (etapa == null) {
			conversas.put(contato, new EtapaConversa(Fase.AGUARDANDO_CONFIRMACAO, null));
			responder(mensagem, PERGUNTA_CONFIRMACAO);
			return;
		}

		if (etapa.fase() == Fase.AGUARDANDO_CONFIRMACAO) {
			String resposta = normalizar(mensagem.texto());
			if ("sim".equals(resposta)) {
				conversas.put(contato, new EtapaConversa(Fase.AGUARDANDO_TITULO, null));
				responder(mensagem, PEDIDO_TITULO);
			} else if ("nao".equals(resposta)) {
				conversas.remove(contato);
				responder(mensagem, ANUNCIO_CANCELADO);
			} else {
				responder(mensagem, PERGUNTA_CONFIRMACAO);
			}
			return;
		}

		if (etapa.fase() == Fase.AGUARDANDO_TITULO) {
			if (mensagem.texto() == null || mensagem.texto().isBlank()) {
				responder(mensagem, PEDIDO_TITULO_NOVAMENTE);
				return;
			}
			conversas.put(contato, new EtapaConversa(Fase.AGUARDANDO_TEXTO, mensagem.texto().trim()));
			responder(mensagem, PEDIDO_TEXTO);
			return;
		}

		if (mensagem.texto() == null || mensagem.texto().isBlank()) {
			responder(mensagem, PEDIDO_TEXTO_NOVAMENTE);
			return;
		}
		anuncioService.criarDeMensagem(mensagem, etapa.titulo(), mensagem.texto().trim());
		conversas.remove(contato);
		responder(mensagem, ANUNCIO_CRIADO);
	}

	private String normalizar(String texto) {
		String semAcentos = Normalizer.normalize(texto.trim().toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
				.replaceAll("\\p{M}", "");
		return semAcentos;
	}

	private void responder(MensagemRecebida mensagem, String texto) {
		MensagemEnviadaDTO resposta = new MensagemEnviadaDTO(mensagem.numero(), texto);
		evolutionClient.enviarMensagem(mensagem.instancia(), resposta);
	}

	private enum Fase {
		AGUARDANDO_CONFIRMACAO,
		AGUARDANDO_TITULO,
		AGUARDANDO_TEXTO
	}

	private record EtapaConversa(Fase fase, String titulo) {
	}

	private record Contato(String instancia, String numero) {
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
