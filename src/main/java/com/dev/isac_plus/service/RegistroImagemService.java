package com.dev.isac_plus.service;

import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dev.isac_plus.dto.ImagemRecebida;
import com.dev.isac_plus.model.Anuncio;
import com.dev.isac_plus.model.ImagemAnuncio;
import com.dev.isac_plus.model.StatusImagem;
import com.dev.isac_plus.repository.AnuncioRepository;
import com.dev.isac_plus.repository.ImagemAnuncioRepository;

/**
 * Recebe a imagem do webhook, decide a qual anuncio ela pertence e grava como PENDENTE.
 * O download fica para o {@link ProcessadorImagens}, para o webhook responder rapido.
 *
 * Precisa rodar com a trava do numero ({@link TravaPorNumero}): fotos de um album chegam ao mesmo
 * tempo e, sem a trava, cada uma criaria o seu proprio rascunho.
 */
@Service
public class RegistroImagemService {

	private static final Logger log = LoggerFactory.getLogger(RegistroImagemService.class);

	private static final List<StatusImagem> CONTAM_NO_LIMITE = List.of(StatusImagem.PENDENTE, StatusImagem.SALVA);

	private final ImagemAnuncioRepository imagens;
	private final AnuncioRepository anuncios;
	private final AnuncioService anuncioService;
	private final ValidadorImagem validador;

	public RegistroImagemService(ImagemAnuncioRepository imagens, AnuncioRepository anuncios,
			AnuncioService anuncioService, ValidadorImagem validador) {
		this.imagens = imagens;
		this.anuncios = anuncios;
		this.anuncioService = anuncioService;
		this.validador = validador;
	}

	@Transactional
	public Optional<ImagemAnuncio> registrar(ImagemRecebida recebida) {
		if (imagens.existsByIdWhatsapp(recebida.idWhatsapp())) {
			log.info("Imagem {} ja estava registrada, ignorando reenvio", recebida.idWhatsapp());
			return Optional.empty();
		}

		Anuncio anuncio = escolherAnuncio(recebida);
		long jaRegistradas = imagens.countByAnuncioId(anuncio.getId());
		ImagemAnuncio imagem = new ImagemAnuncio(anuncio, recebida.idWhatsapp(), recebida.idAlbum(),
				recebida.instancia(), recebida.numero(), recebida.metadados(), (int) jaRegistradas,
				recebida.payload());

		Optional<String> motivo = validador
				.validarMetadados(recebida.metadados().contentType(), recebida.metadados().tamanhoBytes())
				.or(() -> validador.validarQuantidade(imagens.countByAnuncioIdAndStatusIn(anuncio.getId(),
						CONTAM_NO_LIMITE)));
		motivo.ifPresent(imagem::rejeitar);

		ImagemAnuncio salva = imagens.save(imagem);
		log.info("Imagem {} registrada no anuncio {} como {}{}", recebida.idWhatsapp(), anuncio.getId(),
				salva.getStatus(), motivo.map(m -> " (" + m + ")").orElse(""));
		return Optional.of(salva);
	}

	private Anuncio escolherAnuncio(ImagemRecebida recebida) {
		// 1) Album que ja tem foto registrada: todas as fotos dele vao para o mesmo anuncio
		if (recebida.idAlbum() != null) {
			Optional<Anuncio> doAlbum = imagens.findFirstByIdAlbumWhatsappOrderByIdAsc(recebida.idAlbum())
					.map(ImagemAnuncio::getAnuncio);
			if (doAlbum.isPresent()) {
				Anuncio anuncio = doAlbum.get();
				// A legenda pode vir em qualquer foto do album
				if (recebida.legenda() != null && anuncio.isRascunho()) {
					anuncio.publicarComDescricao(recebida.legenda(), recebida.idWhatsapp());
				}
				return anuncio;
			}
		}

		// 2) Foto com legenda: e um anuncio novo, com a legenda como descricao
		if (recebida.legenda() != null) {
			return anuncios.save(new Anuncio("", recebida.legenda(), recebida.numero(), recebida.idWhatsapp()));
		}

		// 3) Sem legenda: tenta o anuncio recente do mesmo numero
		Optional<Anuncio> recente = anuncioService.maisRecente(recebida.numero());
		if (recente.isPresent() && aceitaFotoSemLegenda(recente.get(), recebida)) {
			return recente.get();
		}

		// 4) Nada para ligar: rascunho ate a pessoa mandar o texto
		return anuncios.save(Anuncio.rascunho(recebida.numero()));
	}

	/**
	 * Foto solta sempre entra no anuncio recente (a pessoa manda o texto e depois as fotos uma a uma).
	 * Um album novo so entra se o anuncio ainda nao tem fotos: se ja tem, e provavel que seja outro produto.
	 */
	private boolean aceitaFotoSemLegenda(Anuncio recente, ImagemRecebida recebida) {
		return recebida.idAlbum() == null || imagens.countByAnuncioId(recente.getId()) == 0;
	}
}
