package com.dev.isac_plus.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dev.isac_plus.dto.AnuncioRequest;
import com.dev.isac_plus.dto.AnuncioResponse;
import com.dev.isac_plus.dto.MensagemRecebida;
import com.dev.isac_plus.exception.RecursoNaoEncontradoException;
import com.dev.isac_plus.mapper.AnuncioMapper;
import com.dev.isac_plus.model.Anuncio;
import com.dev.isac_plus.repository.AnuncioRepository;

@Service
public class AnuncioService {

	private final AnuncioRepository repository;
	private final AnuncioMapper mapper;

	public AnuncioService(AnuncioRepository repository, AnuncioMapper mapper) {
		this.repository = repository;
		this.mapper = mapper;
	}

	@Transactional(readOnly = true)
	public List<AnuncioResponse> listar() {
		return repository.findAll().stream().map(mapper::toResponse).toList();
	}

	@Transactional(readOnly = true)
	public AnuncioResponse buscarPorId(Long id) {
		return mapper.toResponse(buscarEntidade(id));
	}

	@Transactional
	public AnuncioResponse criar(AnuncioRequest request) {
		Anuncio salvo = repository.save(mapper.toEntity(request));
		return mapper.toResponse(salvo);
	}

	@Transactional
	public Optional<AnuncioResponse> criarDeMensagem(MensagemRecebida mensagem) {
		if (mensagem.idWhatsapp() != null && repository.existsByIdWhatsapp(mensagem.idWhatsapp())) {
			return Optional.empty();
		}

		Anuncio anuncio = new Anuncio("", mensagem.texto(), mensagem.numero(), mensagem.idWhatsapp());
		return Optional.of(mapper.toResponse(repository.save(anuncio)));
	}

	@Transactional
	public AnuncioResponse atualizar(Long id, AnuncioRequest request) {
		Anuncio anuncio = buscarEntidade(id);
		mapper.atualizarEntity(anuncio, request);
		return mapper.toResponse(repository.save(anuncio));
	}

	@Transactional
	public void excluir(Long id) {
		repository.delete(buscarEntidade(id));
	}

	private Anuncio buscarEntidade(Long id) {
		return repository.findById(id).orElseThrow(() -> new RecursoNaoEncontradoException("Anuncio", id));
	}
}
