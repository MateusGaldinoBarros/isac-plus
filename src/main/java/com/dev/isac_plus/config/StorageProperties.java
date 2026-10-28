package com.dev.isac_plus.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;

/**
 * Dados de acesso ao storage S3, lidos de {@code storage.*} no application.properties.
 */
@Validated
@ConfigurationProperties(prefix = "storage")
public record StorageProperties(
		@NotBlank String endpoint,
		@NotBlank String accessKey,
		@NotBlank String secretKey,
		@NotBlank String bucket,
		@NotBlank String region) {
}
