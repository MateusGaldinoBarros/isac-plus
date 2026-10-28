package com.dev.isac_plus.controller;

import java.time.Duration;

import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.dev.isac_plus.dto.ArquivoImagem;
import com.dev.isac_plus.service.ImagemService;

/**
 * Serve as imagens para o front. O storage fica privado e fora do alcance do navegador;
 * o front so conhece {@code /imagens/{id}}.
 */
@RestController
@RequestMapping("/imagens")
public class ImagemController {

	// O arquivo de uma imagem nunca muda (cada upload ganha uma chave nova), entao o cache pode ser longo
	private static final CacheControl CACHE = CacheControl.maxAge(Duration.ofDays(365)).cachePublic().immutable();

	private final ImagemService service;

	public ImagemController(ImagemService service) {
		this.service = service;
	}

	@GetMapping("/{id}")
	public ResponseEntity<byte[]> buscar(@PathVariable Long id) {
		ArquivoImagem arquivo = service.buscarArquivo(id);
		return ResponseEntity.ok()
				.contentType(MediaType.parseMediaType(arquivo.contentType()))
				.contentLength(arquivo.conteudo().length)
				.cacheControl(CACHE)
				.header("X-Content-Type-Options", "nosniff")
				.body(arquivo.conteudo());
	}
}
