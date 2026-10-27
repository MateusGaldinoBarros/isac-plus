package com.dev.isac_plus.dto;

/** Conteudo da imagem e o tipo para o cabecalho Content-Type. */
public record ArquivoImagem(byte[] conteudo, String contentType) {
}
