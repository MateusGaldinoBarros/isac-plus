package com.dev.isac_plus.repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;

import com.dev.isac_plus.model.ImagemAnuncio;
import com.dev.isac_plus.model.StatusImagem;

public interface ImagemAnuncioRepository extends JpaRepository<ImagemAnuncio, Long> {

	boolean existsByIdWhatsapp(String idWhatsapp);

	/** Imagens com o status dado cuja hora de tentar ja chegou, das mais antigas para as mais novas. */
	List<ImagemAnuncio> findByStatusAndProximaTentativaEmLessThanEqualOrderByProximaTentativaEmAsc(
			StatusImagem status, Instant agora, Limit limite);

	/** Para o limite de imagens por anuncio. */
	long countByAnuncioIdAndStatusIn(Long anuncioId, Collection<StatusImagem> status);

	default List<ImagemAnuncio> pendentesProntasParaTentar(Instant agora, int limite) {
		return findByStatusAndProximaTentativaEmLessThanEqualOrderByProximaTentativaEmAsc(StatusImagem.PENDENTE,
				agora, Limit.of(limite));
	}
}
