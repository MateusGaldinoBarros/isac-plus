package com.dev.isac_plus.dto;

import java.util.List;

/** {@code imagens} traz so as que ja estao salvas no storage, na ordem em que chegaram. */
public record AnuncioResponse(Long id, String titulo, String descricao, String contato, List<ImagemResponse> imagens) {

	public AnuncioResponse(Long id, String titulo, String descricao, String contato) {
		this(id, titulo, descricao, contato, List.of());
	}
}
