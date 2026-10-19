package com.dev.isac_plus.mapper;

import org.springframework.stereotype.Component;

import com.dev.isac_plus.dto.AnuncioRequest;
import com.dev.isac_plus.dto.AnuncioResponse;
import com.dev.isac_plus.model.Anuncio;

@Component
public class AnuncioMapper {

	public Anuncio toEntity(AnuncioRequest request) {
		return new Anuncio(request.titulo(), request.descricao(), request.contato());
	}

	public void atualizarEntity(Anuncio anuncio, AnuncioRequest request) {
		anuncio.setTitulo(request.titulo());
		anuncio.setDescricao(request.descricao());
		anuncio.setContato(request.contato());
	}

	public AnuncioResponse toResponse(Anuncio anuncio) {
		return new AnuncioResponse(anuncio.getId(), anuncio.getTitulo(), anuncio.getDescricao(), anuncio.getContato());
	}
}
