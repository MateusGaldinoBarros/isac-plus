package com.dev.isac_plus.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.dev.isac_plus.config.StorageProperties;
import com.dev.isac_plus.exception.RecursoNaoEncontradoException;
import com.dev.isac_plus.exception.StorageException;

import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketResponse;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@ExtendWith(MockitoExtension.class)
class StorageServiceTest {

	private static final String BUCKET = "bucket-teste";

	@Mock
	private S3Client s3;

	private StorageService service;

	@BeforeEach
	void setUp() {
		service = new StorageService(s3,
				new StorageProperties("http://localhost:8333", "chave", "segredo", BUCKET, "us-east-1"));
	}

	@Test
	void enviarMandaBucketChaveETipo() {
		byte[] conteudo = "imagem".getBytes(StandardCharsets.UTF_8);

		service.enviar("anuncios/1/a.jpg", conteudo, "image/jpeg");

		ArgumentCaptor<PutObjectRequest> captor = ArgumentCaptor.forClass(PutObjectRequest.class);
		verify(s3).putObject(captor.capture(), any(RequestBody.class));
		assertThat(captor.getValue().bucket()).isEqualTo(BUCKET);
		assertThat(captor.getValue().key()).isEqualTo("anuncios/1/a.jpg");
		assertThat(captor.getValue().contentType()).isEqualTo("image/jpeg");
		assertThat(captor.getValue().contentLength()).isEqualTo(conteudo.length);
	}

	@Test
	void enviarComStorageForaDoArLancaStorageException() {
		when(s3.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
				.thenThrow(SdkClientException.create("Connection refused"));

		assertThatThrownBy(() -> service.enviar("anuncios/1/a.jpg", new byte[] { 1 }, "image/jpeg"))
				.isInstanceOf(StorageException.class)
				.hasMessageContaining("anuncios/1/a.jpg")
				.hasCauseInstanceOf(SdkClientException.class);
	}

	@Test
	void baixarDevolveOsBytesDoArquivo() {
		byte[] conteudo = { 1, 2, 3 };
		when(s3.getObjectAsBytes(any(GetObjectRequest.class)))
				.thenReturn(ResponseBytes.fromByteArray(GetObjectResponse.builder().build(), conteudo));

		assertThat(service.baixar("anuncios/1/a.jpg")).containsExactly(1, 2, 3);

		ArgumentCaptor<GetObjectRequest> captor = ArgumentCaptor.forClass(GetObjectRequest.class);
		verify(s3).getObjectAsBytes(captor.capture());
		assertThat(captor.getValue().bucket()).isEqualTo(BUCKET);
		assertThat(captor.getValue().key()).isEqualTo("anuncios/1/a.jpg");
	}

	@Test
	void baixarChaveInexistenteLancaRecursoNaoEncontrado() {
		when(s3.getObjectAsBytes(any(GetObjectRequest.class)))
				.thenThrow(NoSuchKeyException.builder().message("nao existe").build());

		assertThatThrownBy(() -> service.baixar("anuncios/1/x.jpg"))
				.isInstanceOf(RecursoNaoEncontradoException.class);
	}

	@Test
	void baixarComStorageForaDoArLancaStorageException() {
		when(s3.getObjectAsBytes(any(GetObjectRequest.class)))
				.thenThrow(SdkClientException.create("timeout"));

		assertThatThrownBy(() -> service.baixar("anuncios/1/a.jpg")).isInstanceOf(StorageException.class);
	}

	@Test
	void apagarMandaBucketEChave() {
		service.apagar("anuncios/1/a.jpg");

		ArgumentCaptor<DeleteObjectRequest> captor = ArgumentCaptor.forClass(DeleteObjectRequest.class);
		verify(s3).deleteObject(captor.capture());
		assertThat(captor.getValue().bucket()).isEqualTo(BUCKET);
		assertThat(captor.getValue().key()).isEqualTo("anuncios/1/a.jpg");
	}

	@Test
	void apagarComStorageForaDoArLancaStorageException() {
		when(s3.deleteObject(any(DeleteObjectRequest.class))).thenThrow(SdkClientException.create("timeout"));

		assertThatThrownBy(() -> service.apagar("anuncios/1/a.jpg")).isInstanceOf(StorageException.class);
	}

	@Test
	void naoCriaBucketQuandoJaExiste() {
		when(s3.headBucket(any(HeadBucketRequest.class))).thenReturn(HeadBucketResponse.builder().build());

		service.criarBucketSeNaoExistir();

		verify(s3, never()).createBucket(any(CreateBucketRequest.class));
	}

	@Test
	void criaBucketQuandoNaoExiste() {
		when(s3.headBucket(any(HeadBucketRequest.class)))
				.thenThrow(NoSuchBucketException.builder().message("nao existe").build());

		service.criarBucketSeNaoExistir();

		ArgumentCaptor<CreateBucketRequest> captor = ArgumentCaptor.forClass(CreateBucketRequest.class);
		verify(s3).createBucket(captor.capture());
		assertThat(captor.getValue().bucket()).isEqualTo(BUCKET);
	}

	@Test
	void storageForaDoArAoIniciarNaoDerrubaAAplicacao() {
		when(s3.headBucket(any(HeadBucketRequest.class))).thenThrow(SdkClientException.create("Connection refused"));

		service.criarBucketSeNaoExistir();

		verify(s3, never()).createBucket(any(CreateBucketRequest.class));
	}
}
