package com.dev.isac_plus.exception;

/**
 * A imagem quebra uma regra (tipo, tamanho, conteudo). Tentar de novo nao resolve: vira REJEITADA.
 */
public class ImagemInvalidaException extends RuntimeException {

	public ImagemInvalidaException(String motivo) {
		super(motivo);
	}
}
