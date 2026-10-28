package com.dev.isac_plus.dto;

/**
 * Arquivo de midia ja decodificado, vindo da Evolution. Largura e altura podem ser nulas.
 */
public record MidiaBaixada(byte[] conteudo, String mimetype, String nomeArquivo, Integer largura, Integer altura) {
}
