package com.dev.isac_plus.exception;

/**
 * Falha ao obter algo da Evolution API (fora do ar, timeout, midia indisponivel...).
 * Quem chama pode tentar de novo depois.
 */
public class EvolutionException extends RuntimeException {

	public EvolutionException(String mensagem) {
		super(mensagem);
	}

	public EvolutionException(String mensagem, Throwable causa) {
		super(mensagem, causa);
	}
}
