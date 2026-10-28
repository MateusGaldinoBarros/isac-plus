package com.dev.isac_plus.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Dados de acesso a Evolution API, lidos de {@code evolution.*} no application.properties.
 */
@Validated
@ConfigurationProperties(prefix = "evolution")
public record EvolutionProperties(
		@NotBlank String url,
		@NotBlank String apiKey,
		@NotNull Duration timeout) {
}
