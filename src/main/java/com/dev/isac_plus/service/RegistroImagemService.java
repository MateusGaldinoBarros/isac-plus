package com.dev.isac_plus.service;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dev.isac_plus.dto.ImagemRecebida;
import com.dev.isac_plus.model.Anuncio;
import com.dev.isac_plus.model.ImagemAnuncio;
import com.dev.isac_plus.model.StatusImagem;
import com.dev.isac_plus.repository.ImagemAnuncioRepository;

/**
 * Recebe a imagem do webhook para um anuncio escolhido pelo estado da conversa e grava como PENDENTE.
 * O download fica para o {@link ProcessadorImagens}, para o webhook responder rapido.
 *
 * Precisa rodar com a trava do numero ({@link TravaPorNumero}) para manter ordenacao e limite
 * consistentes quando varias fotos chegam ao mesmo tempo.
 */
@Service
public class RegistroImagemService {

	private static final Logger log = LoggerFactory.getLogger(RegistroImagemService.class);

	private static final List<StatusImagem> CONTAM_NO_LIMITE = List.of(StatusImagem.PENDENTE, StatusImagem.SALVA);

	private final ImagemAnuncioRepository imagens;
	private final ValidadorImagem validador;

	public RegistroImagemService(ImagemAnuncioRepository imagens, ValidadorImagem validador) {
		this.imagens = imagens;
		this.validador = validador;
	}

	@Transactional
	public Optional<ImagemAnuncio> registrar(ImagemRecebida recebida, Supplier<Anuncio> escolherAnuncio) {
		if (imagens.existsByIdWhatsapp(recebida.idWhatsapp())) {
			log.info("Imagem {} ja estava registrada, ignorando reenvio", recebida.idWhatsapp());
			return Optional.empty();
		}

		Anuncio anuncio = escolherAnuncio.get();
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
}
