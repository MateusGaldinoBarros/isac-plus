package com.dev.isac_plus.service;

import java.time.Instant;
import java.util.Locale;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dev.isac_plus.dto.EvolutionWebhookEvento;
import com.dev.isac_plus.dto.ImagemRecebida;
import com.dev.isac_plus.dto.MensagemRecebida;
import com.dev.isac_plus.model.Mensagem;
import com.dev.isac_plus.model.MetadadosImagem;
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
		mensagem.ifPresent(this::salvar);
		return mensagem;
	}

	/** Numero de quem mandou a mensagem, quando e uma mensagem recebida de um contato. */
	public Optional<String> numeroDoRemetente(EvolutionWebhookEvento evento) {
		return mensagemRecebida(evento).map(data -> extrairNumero(data.path("key")));
	}

	/**
	 * Extrai a imagem do webhook: foto normal ({@code imageMessage}) ou imagem mandada como documento.
	 * Figurinha, video, audio e documentos que nao sao imagem ficam de fora.
	 */
	public Optional<ImagemRecebida> extrairImagem(EvolutionWebhookEvento evento) {
		Optional<JsonNode> recebida = mensagemRecebida(evento);
		if (recebida.isEmpty() || evento.instance() == null || evento.instance().isBlank()) {
			return Optional.empty();
		}
		JsonNode data = recebida.get();
		JsonNode key = data.path("key");
		String id = textoOuNulo(key.path("id"));
		String numero = extrairNumero(key);
		JsonNode message = data.path("message");
		JsonNode imagem = noDaImagem(message);
		if (id == null || numero == null || imagem == null) {
			return Optional.empty();
		}

		MetadadosImagem metadados = new MetadadosImagem(textoOuNulo(imagem.path("mimetype")),
				longOuNulo(imagem.path("fileLength")), inteiroOuNulo(imagem.path("width")),
				inteiroOuNulo(imagem.path("height")));
		// Fotos de um album apontam para a mensagem pai (albumMessage)
		String idAlbum = textoOuNulo(
				message.path("messageContextInfo").path("messageAssociation").path("parentMessageKey").path("id"));
		return Optional.of(new ImagemRecebida(id, evento.instance(), numero, textoOuNulo(imagem.path("caption")),
				idAlbum, metadados, data.toString()));
	}

	private JsonNode noDaImagem(JsonNode message) {
		JsonNode imagem = message.path("imageMessage");
		if (imagem.isObject()) {
			return imagem;
		}
		JsonNode documento = message.path("documentMessage");
		if (!documento.isObject()) {
			documento = message.path("documentWithCaptionMessage").path("message").path("documentMessage");
		}
		String mimetype = textoOuNulo(documento.path("mimetype"));
		if (documento.isObject() && mimetype != null && mimetype.toLowerCase(Locale.ROOT).startsWith("image/")) {
			return documento;
		}
		return null;
	}

	private Optional<JsonNode> mensagemRecebida(EvolutionWebhookEvento evento) {
		if (evento == null || !EVENTO_MENSAGEM.equals(evento.event()) || evento.data() == null
				|| !evento.data().isObject()) {
			return Optional.empty();
		}
		JsonNode data = evento.data();
		if (data.path("key").path("fromMe").asBoolean(false) || extrairNumero(data.path("key")) == null) {
			return Optional.empty();
		}
		return Optional.of(data);
	}

	private void salvar(MensagemRecebida m) {
		if (m.idWhatsapp() != null && repository.existsByIdWhatsapp(m.idWhatsapp())) {
			log.info("Mensagem {} ja estava salva, ignorando reenvio", m.idWhatsapp());
			return;
		}
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

	// O Baileys manda o tamanho como numero, como texto ou como Long do protobuf ({low, high})
	private Long longOuNulo(JsonNode no) {
		if (no.isIntegralNumber()) {
			return no.asLong();
		}
		if (no.isString()) {
			try {
				return Long.valueOf(no.asString().trim());
			} catch (NumberFormatException e) {
				return null;
			}
		}
		if (no.isObject() && no.path("low").isIntegralNumber()) {
			long low = no.path("low").asLong() & 0xFFFFFFFFL;
			long high = no.path("high").asLong(0) & 0xFFFFFFFFL;
			return (high << 32) | low;
		}
		return null;
	}

	private Integer inteiroOuNulo(JsonNode no) {
		Long valor = longOuNulo(no);
		return valor == null || valor > Integer.MAX_VALUE ? null : valor.intValue();
	}
}
