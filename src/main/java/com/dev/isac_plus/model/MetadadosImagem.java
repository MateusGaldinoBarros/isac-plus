package com.dev.isac_plus.model;

/**
 * Tipo, tamanho e dimensoes de uma imagem. Qualquer campo pode ser nulo quando a origem nao informa.
 */
public record MetadadosImagem(String contentType, Long tamanhoBytes, Integer largura, Integer altura) {
}
