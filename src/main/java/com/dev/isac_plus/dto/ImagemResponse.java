package com.dev.isac_plus.dto;

/** Imagem pronta para o front: {@code url} vai direto num {@code <img src>}. */
public record ImagemResponse(Long id, String url, Integer largura, Integer altura, int ordem) {
}
