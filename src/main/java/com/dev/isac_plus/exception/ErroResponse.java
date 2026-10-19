package com.dev.isac_plus.exception;

import java.time.Instant;
import java.util.Map;

public record ErroResponse(int status, String mensagem, Map<String, String> campos, Instant timestamp) {

	public ErroResponse(int status, String mensagem) {
		this(status, mensagem, Map.of(), Instant.now());
	}

	public ErroResponse(int status, String mensagem, Map<String, String> campos) {
		this(status, mensagem, campos, Instant.now());
	}
}
