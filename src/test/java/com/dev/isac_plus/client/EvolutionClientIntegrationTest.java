package com.dev.isac_plus.client;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Properties;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import com.dev.isac_plus.config.EvolutionConfig;
import com.dev.isac_plus.config.EvolutionProperties;
import com.dev.isac_plus.exception.EvolutionException;

import tools.jackson.databind.json.JsonMapper;

/**
 * Roda contra a Evolution de verdade ({@code docker compose up -d evolution-api}) usando o .env.local.
 * Sem um WhatsApp conectado nao da para baixar uma foto real, entao confere o caminho de erro:
 * se a apikey ou a URL estivessem erradas a resposta seria 401 ou outra, nao "instance does not exist".
 */
class EvolutionClientIntegrationTest {

	private static EvolutionClient client;

	@BeforeAll
	static void conectar() throws IOException {
		Properties env = new Properties();
		Path arquivo = Path.of(".env.local");
		if (Files.exists(arquivo)) {
			try (InputStream in = Files.newInputStream(arquivo)) {
				env.load(in);
			}
		}
		String url = env.getProperty("EVOLUTION_URL", "http://localhost:8080");
		String apiKey = env.getProperty("EVOLUTION_API_KEY", "");
		assumeTrue(!apiKey.isBlank(), "EVOLUTION_API_KEY nao configurada");
		assumeTrue(evolutionNoAr(url), "Evolution nao esta rodando em " + url);

		client = new EvolutionClient(EvolutionConfig.configurar(RestClient.builder(),
				new EvolutionProperties(url, apiKey, Duration.ofSeconds(10))).build());
	}

	@Test
	void instanciaInexistenteRespondeComOMotivoDaEvolution() {
		var data = JsonMapper.builder().build().readTree("{\"key\":{\"id\":\"TESTE\"}}");

		assertThatThrownBy(() -> client.baixarMidia("instancia-que-nao-existe", data))
				.isInstanceOf(EvolutionException.class)
				.hasMessageContaining("404")
				.hasMessageContaining("instance does not exist");
	}

	private static boolean evolutionNoAr(String url) {
		try {
			HttpResponse<Void> resposta = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build()
					.send(HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(2)).build(),
							HttpResponse.BodyHandlers.discarding());
			return resposta.statusCode() < 500;
		} catch (IOException e) {
			return false;
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			return false;
		}
	}
}
