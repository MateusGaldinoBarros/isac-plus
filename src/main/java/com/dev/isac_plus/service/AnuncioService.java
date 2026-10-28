package com.dev.isac_plus.service;

import java.time.Clock;
import java.time.Duration;
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

	/**
	 * Tempo em que fotos e texto do mesmo numero sao tratados como o mesmo anuncio,
	 * contado a partir da criacao do anuncio.
	 */
	public static final Duration JANELA_AGRUPAMENTO = Duration.ofMinutes(10);

	private final AnuncioRepository repository;
	private final AnuncioMapper mapper;
	private final Clock relogio;

	public AnuncioService(AnuncioRepository repository, AnuncioMapper mapper, Clock relogio) {
		this.repository = repository;
		this.mapper = mapper;
		this.relogio = relogio;
	}

	/** Rascunhos ficam de fora: ainda nao tem descricao. */
	@Transactional(readOnly = true)
	public List<AnuncioResponse> listar() {
		return repository.findByRascunhoFalse().stream().map(mapper::toResponse).toList();
	}

	@Transactional(readOnly = true)
	public AnuncioResponse buscarPorId(Long id) {
		return mapper.toResponse(repository.findByIdAndRascunhoFalse(id)
				.orElseThrow(() -> new RecursoNaoEncontradoException("Anuncio", id)));
	}

	@Transactional
	public AnuncioResponse criar(AnuncioRequest request) {
		Anuncio salvo = repository.save(mapper.toEntity(request));
		return mapper.toResponse(salvo);
	}

	@Transactional
	public Optional<AnuncioResponse> criarDeMensagem(MensagemRecebida mensagem) {
		return criarDeMensagem(mensagem, "", mensagem.texto());
	}

	@Transactional
	public Optional<AnuncioResponse> criarDeMensagem(MensagemRecebida mensagem, String titulo, String descricao) {
		if (mensagem.idWhatsapp() != null && repository.existsByIdWhatsapp(mensagem.idWhatsapp())) {
			return Optional.empty();
		}

		
		// Fotos chegaram antes do texto: o texto completa o rascunho em vez de criar outro anuncio
		Optional<Anuncio> rascunho = maisRecente(mensagem.numero()).filter(Anuncio::isRascunho);
		if (rascunho.isPresent()) {
			rascunho.get().publicarComDescricao(mensagem.texto(), mensagem.idWhatsapp());
			return Optional.of(mapper.toResponse(repository.save(rascunho.get())));
		}

		Anuncio anuncio = new Anuncio(titulo, descricao, mensagem.numero(), mensagem.idWhatsapp());
		return Optional.of(mapper.toResponse(repository.save(anuncio)));
	}

	/** Anuncio mais recente do numero dentro da janela de agrupamento. */
	@Transactional(readOnly = true)
	public Optional<Anuncio> maisRecente(String numero) {
		return repository.maisRecenteDoNumeroDesde(numero, relogio.instant().minus(JANELA_AGRUPAMENTO));
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
