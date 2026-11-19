package com.dev.isac_plus.exception;

import java.time.Duration;

/** Indica que o contato ainda esta dentro do intervalo entre publicacoes. */
public class TimeoutCriacaoAnuncioException extends RuntimeException {

	private final Duration tempoRestante;

	public TimeoutCriacaoAnuncioException(Duration tempoRestante) {
		super("Você já publicou um anúncio recentemente. Tente novamente em "
				+ formatar(tempoRestante) + ".");
		this.tempoRestante = tempoRestante;
	}

	public Duration getTempoRestante() {
		return tempoRestante;
	}

	private static String formatar(Duration tempo) {
		long minutos = Math.max(1, (tempo.toSeconds() + 59) / 60);
		return minutos == 1 ? "1 minuto" : minutos + " minutos";
	}
}
