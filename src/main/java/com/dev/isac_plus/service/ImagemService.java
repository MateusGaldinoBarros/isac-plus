package com.dev.isac_plus.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dev.isac_plus.dto.ArquivoImagem;
import com.dev.isac_plus.exception.RecursoNaoEncontradoException;
import com.dev.isac_plus.model.ImagemAnuncio;
import com.dev.isac_plus.model.StatusImagem;
import com.dev.isac_plus.repository.ImagemAnuncioRepository;

@Service
public class ImagemService {

	private final ImagemAnuncioRepository repository;
	private final StorageService storage;

	public ImagemService(ImagemAnuncioRepository repository, StorageService storage) {
		this.repository = repository;
		this.storage = storage;
	}

	/**
	 * So entrega imagens SALVA de anuncios publicados. Pendentes, rejeitadas e as de rascunho
	 * respondem 404, igual a um id que nao existe.
	 */
	@Transactional(readOnly = true)
	public ArquivoImagem buscarArquivo(Long id) {
		ImagemAnuncio imagem = repository.findByIdAndStatusAndAnuncioRascunhoFalse(id, StatusImagem.SALVA)
				.orElseThrow(() -> new RecursoNaoEncontradoException("Imagem", id));
		return new ArquivoImagem(storage.baixar(imagem.getChaveStorage()), imagem.getContentType());
	}
}
