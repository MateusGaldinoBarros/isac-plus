package com.dev.isac_plus.service;

import static org.assertj.core.api.Assertions.assertThat;
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
import java.util.UUID;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.dev.isac_plus.config.StorageConfig;
import com.dev.isac_plus.config.StorageProperties;
import com.dev.isac_plus.exception.RecursoNaoEncontradoException;

import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteBucketRequest;

/**
 * Roda contra o SeaweedFS de verdade ({@code docker compose up -d seaweedfs}).
 * Usa as chaves do .env.local e um bucket proprio, apagado no fim.
 * E pulado quando o storage nao esta rodando ou as chaves nao estao configuradas.
 */
class StorageServiceIntegrationTest {

	private static final String BUCKET = "isac-plus-teste-" + UUID.randomUUID().toString().substring(0, 8);

	private static S3Client s3;
	private static StorageService service;

	@BeforeAll
	static void conectar() throws IOException {
		Properties env = lerEnvLocal();
		String endpoint = env.getProperty("S3_ENDPOINT", "http://localhost:8333");
		String accessKey = env.getProperty("S3_ACCESS_KEY", "");
		String secretKey = env.getProperty("S3_SECRET_KEY", "");
		assumeTrue(!accessKey.isBlank() && !secretKey.isBlank(), "S3_ACCESS_KEY/S3_SECRET_KEY nao configuradas");
		assumeTrue(storageNoAr(endpoint), "Storage nao esta rodando em " + endpoint);

		StorageProperties props = new StorageProperties(endpoint, accessKey, secretKey, BUCKET, "us-east-1");
		s3 = StorageConfig.criarCliente(props);
		service = new StorageService(s3, props);
		service.criarBucketSeNaoExistir();
	}

	@AfterAll
	static void limpar() {
		if (s3 != null) {
			s3.deleteBucket(DeleteBucketRequest.builder().bucket(BUCKET).build());
			s3.close();
		}
	}

	@Test
	void enviaBaixaEApagaUmArquivo() {
		String chave = "anuncios/1/" + UUID.randomUUID() + ".jpg";
		byte[] conteudo = { (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 1, 2, 3 };

		service.enviar(chave, conteudo, "image/jpeg");
		assertThat(service.baixar(chave)).isEqualTo(conteudo);

		service.apagar(chave);
		assertThatThrownBy(() -> service.baixar(chave)).isInstanceOf(RecursoNaoEncontradoException.class);
	}

	@Test
	void criarBucketDeNovoNaoDaErro() {
		service.criarBucketSeNaoExistir();
	}

	@Test
	void apagarChaveInexistenteNaoDaErro() {
		service.apagar("anuncios/1/nao-existe.jpg");
	}

	private static Properties lerEnvLocal() throws IOException {
		Properties props = new Properties();
		Path arquivo = Path.of(".env.local");
		if (Files.exists(arquivo)) {
			try (InputStream in = Files.newInputStream(arquivo)) {
				props.load(in);
			}
		}
		return props;
	}

	private static boolean storageNoAr(String endpoint) {
		try {
			HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
			HttpResponse<Void> resposta = http.send(
					HttpRequest.newBuilder(URI.create(endpoint + "/healthz")).timeout(Duration.ofSeconds(2)).build(),
					HttpResponse.BodyHandlers.discarding());
			return resposta.statusCode() == 200;
		} catch (IOException e) {
			return false;
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			return false;
		}
	}
}
