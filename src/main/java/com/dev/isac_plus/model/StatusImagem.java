package com.dev.isac_plus.model;

public enum StatusImagem {

	/** Registrada pelo webhook, esperando o download. */
	PENDENTE,

	/** Arquivo guardado no storage. */
	SALVA,

	/** Desistimos depois de todas as tentativas. Pode ser reprocessada manualmente. */
	FALHOU,

	/** Fora das regras (tipo, tamanho, limite por anuncio). Nao adianta tentar de novo. */
	REJEITADA
}
