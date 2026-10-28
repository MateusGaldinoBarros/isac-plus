package com.dev.isac_plus.exception;

/**
 * Falha ao falar com o storage (fora do ar, credencial errada, timeout...).
 * Quem chama pode tentar de novo depois.
 */
public class StorageException extends RuntimeException {

	public StorageException(String mensagem, Throwable causa) {
		super(mensagem, causa);
	}
}
