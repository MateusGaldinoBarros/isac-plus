package com.dev.isac_plus.client;

import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import com.dev.isac_plus.dto.MidiaBaixada;
import com.dev.isac_plus.exception.EvolutionException;

import tools.jackson.databind.JsonNode;

/**
 * Fala com a Evolution API. O webhook so traz os metadados da midia; o arquivo vem daqui.
 */
@Component
public class EvolutionClient {

	private final RestClient http;

	public EvolutionClient(@Qualifier("evolutionRestClient") RestClient http) {
		this.http = http;
	}

	/**
	 * Baixa a midia de uma mensagem recebida.
	 *
	 * @param instancia nome da instancia na Evolution (campo {@code instance} do webhook)
	 * @param mensagem  o objeto {@code data} do webhook, inteiro. Assim a Evolution nao precisa
	 *                  procurar a mensagem no banco dela e consegue pedir reenvio ao celular
	 */
	public MidiaBaixada baixarMidia(String instancia, JsonNode mensagem) {
		Map<String, Object> corpo = new LinkedHashMap<>();
		corpo.put("message", mensagem);
		corpo.put("convertToMp4", false);

		JsonNode resposta;
		try {
			resposta = http.post()
					.uri("/chat/getBase64FromMediaMessage/{instancia}", instancia)
					.contentType(MediaType.APPLICATION_JSON)
					.body(corpo)
					.retrieve()
					.body(JsonNode.class);
		} catch (RestClientResponseException e) {
			throw new EvolutionException("Evolution respondeu " + e.getStatusCode().value()
					+ " ao baixar midia: " + e.getResponseBodyAsString(), e);
		} catch (RestClientException e) {
			throw new EvolutionException("Falha ao chamar a Evolution para baixar midia: " + e.getMessage(), e);
		}

		return converter(resposta);
	}

	private MidiaBaixada converter(JsonNode resposta) {
		String base64 = texto(resposta, "base64");
		if (base64 == null) {
			throw new EvolutionException("Evolution nao devolveu o conteudo da midia");
		}
		byte[] conteudo;
		try {
			// Tira quebras de linha, mas recusa qualquer outro caractere fora do base64
			conteudo = Base64.getDecoder().decode(base64.replaceAll("\\s", ""));
		} catch (IllegalArgumentException e) {
			throw new EvolutionException("Base64 invalido na resposta da Evolution", e);
		}
		if (conteudo.length == 0) {
			throw new EvolutionException("Evolution devolveu midia vazia");
		}

		JsonNode tamanho = resposta.path("size");
		return new MidiaBaixada(conteudo, texto(resposta, "mimetype"), texto(resposta, "fileName"),
				inteiro(tamanho.path("width")), inteiro(tamanho.path("height")));
	}

	private String texto(JsonNode no, String campo) {
		if (no == null) {
			return null;
		}
		JsonNode valor = no.path(campo);
		if (!valor.isString() || valor.asString().isBlank()) {
			return null;
		}
		return valor.asString();
	}

	// O Baileys as vezes manda numeros como texto, entao aceitamos os dois
	private Integer inteiro(JsonNode no) {
		if (no.isIntegralNumber()) {
			return no.asInt();
		}
		if (no.isString()) {
			try {
				return Integer.valueOf(no.asString().trim());
			} catch (NumberFormatException e) {
				return null;
			}
		}
		return null;
	}
}
