package com.dev.isac_plus.useCase;

import java.text.Normalizer;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dev.isac_plus.dto.EvolutionWebhookEvento;
import com.dev.isac_plus.dto.MensagemEnviadaDTO;
import com.dev.isac_plus.dto.MensagemRecebida;
import com.dev.isac_plus.restClient.EvolutionClient;
import com.dev.isac_plus.service.AnuncioService;
import com.dev.isac_plus.service.WebhookService;

@Service
public class CriarAnuncioUseCase {

	private final WebhookService webhookService;
	private final AnuncioService anuncioService;
	private final EvolutionClient evolutionClient;
	private final ConcurrentHashMap<Contato, EtapaConversa> conversas = new ConcurrentHashMap<>();

	private static final String PERGUNTA_CONFIRMACAO = "Você gostaria de criar um anúncio? Responda sim ou não.";
	private static final String PEDIDO_TEXTO = "Envie o texto que deseja publicar como anúncio.";
	private static final String ANUNCIO_CRIADO = "Anúncio criado com sucesso!";
	private static final String ANUNCIO_CANCELADO = "Tudo bem, não criarei um anúncio. Envie outra mensagem quando quiser começar.";

	public CriarAnuncioUseCase(WebhookService webhookService, AnuncioService anuncioService, EvolutionClient evolutionClient) {
		this.webhookService = webhookService;
		this.anuncioService = anuncioService;
		this.evolutionClient = evolutionClient;
	}

	@Transactional
	public void executar(EvolutionWebhookEvento evento) {
		webhookService.processar(evento).ifPresent(this::processarMensagem);
	}

	private void processarMensagem(MensagemRecebida mensagem) {
		Contato contato = new Contato(mensagem.instancia(), mensagem.numero());
		EtapaConversa etapa = conversas.get(contato);

		if (etapa == null) {
			conversas.put(contato, EtapaConversa.AGUARDANDO_CONFIRMACAO);
			responder(mensagem, PERGUNTA_CONFIRMACAO);
			return;
		}

		if (etapa == EtapaConversa.AGUARDANDO_CONFIRMACAO) {
			String resposta = normalizar(mensagem.texto());
			if ("sim".equals(resposta)) {
				conversas.put(contato, EtapaConversa.AGUARDANDO_TEXTO);
				responder(mensagem, PEDIDO_TEXTO);
			} else if ("nao".equals(resposta)) {
				conversas.remove(contato);
				responder(mensagem, ANUNCIO_CANCELADO);
			} else {
				responder(mensagem, PERGUNTA_CONFIRMACAO);
			}
			return;
		}

		anuncioService.criarDeMensagem(mensagem);
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

	private enum EtapaConversa {
		AGUARDANDO_CONFIRMACAO,
		AGUARDANDO_TEXTO
	}

	private record Contato(String instancia, String numero) {
	}
}
