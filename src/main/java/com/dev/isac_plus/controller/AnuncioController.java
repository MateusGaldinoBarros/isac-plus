package com.dev.isac_plus.controller;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.dev.isac_plus.dto.AnuncioRequest;
import com.dev.isac_plus.dto.AnuncioResponse;
import com.dev.isac_plus.service.AnuncioService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/anuncios")
public class AnuncioController {

	private final AnuncioService service;

	public AnuncioController(AnuncioService service) {
		this.service = service;
	}

	@GetMapping
	public List<AnuncioResponse> listar() {
		return service.listar();
	}

	@GetMapping("/{id}")
	public AnuncioResponse buscarPorId(@PathVariable Long id) {
		return service.buscarPorId(id);
	}

	@PostMapping
	public ResponseEntity<AnuncioResponse> criar(@Valid @RequestBody AnuncioRequest request) {
		AnuncioResponse criado = service.criar(request);
		return ResponseEntity.created(URI.create("/anuncios/" + criado.id())).body(criado);
	}

	@PutMapping("/{id}")
	public AnuncioResponse atualizar(@PathVariable Long id, @Valid @RequestBody AnuncioRequest request) {
		return service.atualizar(id, request);
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> excluir(@PathVariable Long id) {
		service.excluir(id);
		return ResponseEntity.noContent().build();
	}
}
