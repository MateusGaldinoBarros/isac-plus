package com.dev.isac_plus.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import com.dev.isac_plus.config.StorageProperties;
import com.dev.isac_plus.exception.RecursoNaoEncontradoException;
import com.dev.isac_plus.exception.StorageException;

import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.BucketAlreadyOwnedByYouException;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

/**
 * Guarda e le arquivos no storage S3. As chaves sao caminhos como {@code anuncios/42/7f3c.jpg}.
 */
@Service
public class StorageService {

	private static final Logger log = LoggerFactory.getLogger(StorageService.class);

	private final S3Client s3;
	private final String bucket;

	public StorageService(S3Client s3, StorageProperties props) {
		this.s3 = s3;
		this.bucket = props.bucket();
	}

	/**
	 * Cria o bucket ao iniciar. Se o storage estiver fora do ar, so registra o aviso:
	 * a aplicacao sobe mesmo assim e as imagens ficam pendentes ate o storage voltar.
	 */
	@EventListener(ApplicationReadyEvent.class)
	public void criarBucketSeNaoExistir() {
		try {
			s3.headBucket(HeadBucketRequest.builder().bucket(bucket).build());
		} catch (NoSuchBucketException e) {
			criarBucket();
		} catch (SdkException e) {
			log.warn("Storage indisponivel ao iniciar; o bucket {} nao foi verificado: {}", bucket, e.getMessage());
		}
	}

	private void criarBucket() {
		try {
			s3.createBucket(CreateBucketRequest.builder().bucket(bucket).build());
			log.info("Bucket {} criado no storage", bucket);
		} catch (BucketAlreadyOwnedByYouException e) {
			// Outra instancia criou ao mesmo tempo
		} catch (SdkException e) {
			log.warn("Nao foi possivel criar o bucket {}: {}", bucket, e.getMessage());
		}
	}

	public void enviar(String chave, byte[] conteudo, String contentType) {
		try {
			s3.putObject(PutObjectRequest.builder()
					.bucket(bucket)
					.key(chave)
					.contentType(contentType)
					.contentLength((long) conteudo.length)
					.build(), RequestBody.fromBytes(conteudo));
		} catch (SdkException e) {
			throw new StorageException("Falha ao enviar " + chave + " para o storage", e);
		}
	}

	public byte[] baixar(String chave) {
		try {
			return s3.getObjectAsBytes(GetObjectRequest.builder().bucket(bucket).key(chave).build()).asByteArray();
		} catch (NoSuchKeyException e) {
			throw new RecursoNaoEncontradoException("Arquivo", chave);
		} catch (SdkException e) {
			throw new StorageException("Falha ao baixar " + chave + " do storage", e);
		}
	}

	/** Apagar uma chave que nao existe nao e erro no S3. */
	public void apagar(String chave) {
		try {
			s3.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(chave).build());
		} catch (SdkException e) {
			throw new StorageException("Falha ao apagar " + chave + " do storage", e);
		}
	}
}
