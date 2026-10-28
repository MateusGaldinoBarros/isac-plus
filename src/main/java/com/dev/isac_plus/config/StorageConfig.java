package com.dev.isac_plus.config;

import java.net.URI;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.checksums.RequestChecksumCalculation;
import software.amazon.awssdk.core.checksums.ResponseChecksumValidation;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration
@EnableConfigurationProperties(StorageProperties.class)
public class StorageConfig {

	@Bean
	public S3Client s3Client(StorageProperties props) {
		return criarCliente(props);
	}

	public static S3Client criarCliente(StorageProperties props) {
		return S3Client.builder()
				.endpointOverride(URI.create(props.endpoint()))
				.region(Region.of(props.region()))
				.credentialsProvider(StaticCredentialsProvider.create(
						AwsBasicCredentials.create(props.accessKey(), props.secretKey())))
				// SeaweedFS e outros S3 compativeis usam endpoint/bucket/chave, nao bucket.endpoint
				.forcePathStyle(true)
				// O SDK manda checksums novos por padrao; nem todo S3 compativel aceita
				.requestChecksumCalculation(RequestChecksumCalculation.WHEN_REQUIRED)
				.responseChecksumValidation(ResponseChecksumValidation.WHEN_REQUIRED)
				.build();
	}
}
