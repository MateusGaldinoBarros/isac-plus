package com.dev.isac_plus.dto;

import com.dev.isac_plus.model.MetadadosImagem;

/**
 * Imagem extraida de um webhook. {@code legenda} e {@code idAlbum} podem ser nulos.
 * {@code payload} e o "data" do webhook em JSON, guardado para pedir o arquivo a Evolution depois.
 */
public record ImagemRecebida(String idWhatsapp, String instancia, String numero, String legenda, String idAlbum,
		MetadadosImagem metadados, String payload) {
}
