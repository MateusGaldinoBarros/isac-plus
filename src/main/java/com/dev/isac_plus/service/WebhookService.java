package com.dev.isac_plus.service;

import java.time.Instant;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dev.isac_plus.dto.EvolutionWebhookEvento;
import com.dev.isac_plus.dto.MensagemRecebida;
import com.dev.isac_plus.model.Mensagem;
import com.dev.isac_plus.repository.MensagemRepository;

import tools.jackson.databind.JsonNode;

@Service
public class WebhookService {

	static final String EVENTO_MENSAGEM = "messages.upsert";

	private static final Logger log = LoggerFactory.getLogger(WebhookService.class);

	private final MensagemRepository repository;

	public WebhookService(MensagemRepository repository) {
		this.repository = repository;
	}

	@Transactional
	public Optional<MensagemRecebida> processar(EvolutionWebhookEvento evento) {
		Optional<MensagemRecebida> mensagem = extrair(evento);
		if (mensagem.isEmpty()) {
			return Optional.empty();
		}

		MensagemRecebida recebida = mensagem.get();
		if (recebida.idWhatsapp() != null && repository.existsByIdWhatsapp(recebida.idWhatsapp())) {
			log.info("Mensagem {} ja estava salva, ignorando reenvio", recebida.idWhatsapp());
			return Optional.empty();
		}
		salvar(recebida);
		return mensagem;
	}

	private void salvar(MensagemRecebida m) {
		repository.save(new Mensagem(m.idWhatsapp(), m.instancia(), m.numero(), m.nome(), m.texto(), Instant.now()));
		log.info("Mensagem salva de {} ({}): {}", m.numero(), m.nome(), m.texto());
	}

	private Optional<MensagemRecebida> extrair(EvolutionWebhookEvento evento) {
		if (evento == null || !EVENTO_MENSAGEM.equals(evento.event()) || evento.data() == null) {
			return Optional.empty();
		}

		JsonNode data = evento.data();
		JsonNode key = data.path("key");
		if (key.path("fromMe").asBoolean(false)) {
			return Optional.empty();
		}

		String numero = extrairNumero(key);
		String texto = extrairTexto(data.path("message"));
		if (numero == null || texto == null) {
			return Optional.empty();
		}

		return Optional.of(new MensagemRecebida(textoOuNulo(key.path("id")), evento.instance(), numero,
				textoOuNulo(data.path("pushName")), texto));
	}

	// Contatos com LID trazem o numero real em senderPn ou remoteJidAlt.
	private String extrairNumero(JsonNode key) {
		String jid = textoOuNulo(key.path("remoteJid"));
		if (jid == null || jid.endsWith("@g.us") || jid.endsWith("@broadcast")) {
			return null;
		}
		if (jid.endsWith("@lid")) {
			jid = textoOuNulo(key.path("senderPn"));
			if (jid == null) {
				jid = textoOuNulo(key.path("remoteJidAlt"));
			}
			if (jid == null) {
				return null;
			}
		}
		int arroba = jid.indexOf('@');
		return arroba >= 0 ? jid.substring(0, arroba) : jid;
	}

	private String extrairTexto(JsonNode message) {
		String[][] caminhos = {
				{ "conversation" },
				{ "extendedTextMessage", "text" },
				{ "imageMessage", "caption" },
				{ "videoMessage", "caption" } };
		for (String[] caminho : caminhos) {
			JsonNode no = message;
			for (String campo : caminho) {
				no = no.path(campo);
			}
			String texto = textoOuNulo(no);
			if (texto != null) {
				return texto;
			}
		}
		return null;
	}

	private String textoOuNulo(JsonNode no) {
		if (no == null || !no.isString()) {
			return null;
		}
		String texto = no.asString();
		return texto.isBlank() ? null : texto;
	}
}
