package com.dev.isac_plus.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withResourceNotFound;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.time.Duration;
import java.util.Base64;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.dev.isac_plus.config.EvolutionConfig;
import com.dev.isac_plus.config.EvolutionProperties;
import com.dev.isac_plus.dto.MidiaBaixada;
import com.dev.isac_plus.exception.EvolutionException;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

class EvolutionClientTest {

	private static final String URL = "http://evolution:8080/chat/getBase64FromMediaMessage/ong";
	private static final byte[] JPEG = { (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x10, 0x20 };

	private final JsonMapper json = JsonMapper.builder().build();

	private MockRestServiceServer servidor;
	private EvolutionClient client;

	@BeforeEach
	void setUp() {
		RestClient.Builder builder = EvolutionConfig.configurar(RestClient.builder(),
				new EvolutionProperties("http://evolution:8080", "chave-secreta", Duration.ofSeconds(5)));
		servidor = MockRestServiceServer.bindTo(builder).build();
		client = new EvolutionClient(builder.build());
	}

	@Test
	void enviaOWebhookInteiroComApikeyEDevolveOArquivoDecodificado() {
		servidor.expect(requestTo(URL))
				.andExpect(method(HttpMethod.POST))
				.andExpect(header("apikey", "chave-secreta"))
				.andExpect(content().json("""
						{"message":{"key":{"id":"ABC"},"message":{"imageMessage":{"mimetype":"image/jpeg"}}},
						 "convertToMp4":false}
						""", JsonCompareMode.STRICT))
				.andRespond(withSuccess("""
						{"mediaType":"imageMessage","fileName":"ABC.jpeg","mimetype":"image/jpeg",
						 "size":{"fileLength":"5","width":1080,"height":1350},
						 "base64":"%s"}
						""".formatted(Base64.getEncoder().encodeToString(JPEG)), MediaType.APPLICATION_JSON));

		MidiaBaixada midia = client.baixarMidia("ong", dataDoWebhook());

		assertThat(midia.conteudo()).isEqualTo(JPEG);
		assertThat(midia.mimetype()).isEqualTo("image/jpeg");
		assertThat(midia.nomeArquivo()).isEqualTo("ABC.jpeg");
		assertThat(midia.largura()).isEqualTo(1080);
		assertThat(midia.altura()).isEqualTo(1350);
		servidor.verify();
	}

	@Test
	void aceitaDimensoesComoTextoEAusentes() {
		servidor.expect(requestTo(URL)).andRespond(withSuccess("""
				{"mimetype":"image/png","size":{"width":"640"},"base64":"%s"}
				""".formatted(Base64.getEncoder().encodeToString(JPEG)), MediaType.APPLICATION_JSON));

		MidiaBaixada midia = client.baixarMidia("ong", dataDoWebhook());

		assertThat(midia.largura()).isEqualTo(640);
		assertThat(midia.altura()).isNull();
		assertThat(midia.nomeArquivo()).isNull();
	}

	@Test
	void nomeDaInstanciaEhCodificadoNaUrl() {
		servidor.expect(requestTo("http://evolution:8080/chat/getBase64FromMediaMessage/minha%20ong"))
				.andRespond(withSuccess("{\"base64\":\"AQID\"}", MediaType.APPLICATION_JSON));

		assertThat(client.baixarMidia("minha ong", dataDoWebhook()).conteudo()).containsExactly(1, 2, 3);
	}

	@Test
	void erro404DaEvolutionViraEvolutionExceptionComOMotivo() {
		servidor.expect(requestTo(URL)).andRespond(withResourceNotFound()
				.contentType(MediaType.APPLICATION_JSON)
				.body("{\"status\":404,\"error\":\"Not Found\",\"response\":{\"message\":[\"The \\\"ong\\\" instance does not exist\"]}}"));

		assertThatThrownBy(() -> client.baixarMidia("ong", dataDoWebhook()))
				.isInstanceOf(EvolutionException.class)
				.hasMessageContaining("404")
				.hasMessageContaining("instance does not exist");
	}

	@Test
	void erro500DaEvolutionViraEvolutionException() {
		servidor.expect(requestTo(URL)).andRespond(withServerError());

		assertThatThrownBy(() -> client.baixarMidia("ong", dataDoWebhook()))
				.isInstanceOf(EvolutionException.class)
				.hasMessageContaining("500");
	}

	@Test
	void respostaSemBase64ViraEvolutionException() {
		servidor.expect(requestTo(URL)).andRespond(withSuccess("{\"mimetype\":\"image/jpeg\"}", MediaType.APPLICATION_JSON));

		assertThatThrownBy(() -> client.baixarMidia("ong", dataDoWebhook()))
				.isInstanceOf(EvolutionException.class)
				.hasMessageContaining("conteudo");
	}

	@Test
	void base64InvalidoViraEvolutionException() {
		servidor.expect(requestTo(URL)).andRespond(withSuccess("{\"base64\":\"@@@nao-e-base64@@@\"}", MediaType.APPLICATION_JSON));

		assertThatThrownBy(() -> client.baixarMidia("ong", dataDoWebhook()))
				.isInstanceOf(EvolutionException.class)
				.hasMessageContaining("Base64");
	}

	@Test
	void evolutionForaDoArViraEvolutionException() {
		EvolutionClient semServidor = new EvolutionClient(EvolutionConfig.configurar(RestClient.builder(),
				new EvolutionProperties("http://127.0.0.1:1", "x", Duration.ofSeconds(2))).build());

		assertThatThrownBy(() -> semServidor.baixarMidia("ong", dataDoWebhook()))
				.isInstanceOf(EvolutionException.class)
				.hasMessageContaining("Falha ao chamar a Evolution");
	}

	private JsonNode dataDoWebhook() {
		return json.readTree("""
				{"key":{"id":"ABC"},"message":{"imageMessage":{"mimetype":"image/jpeg"}}}
				""");
	}
}
