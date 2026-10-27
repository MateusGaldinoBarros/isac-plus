package com.dev.isac_plus.repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;

import com.dev.isac_plus.model.ImagemAnuncio;
import com.dev.isac_plus.model.StatusImagem;

public interface ImagemAnuncioRepository extends JpaRepository<ImagemAnuncio, Long> {

	boolean existsByIdWhatsapp(String idWhatsapp);

	/** Imagens com o status dado cuja hora de tentar ja chegou, das mais antigas para as mais novas. */
	List<ImagemAnuncio> findByStatusAndProximaTentativaEmLessThanEqualOrderByProximaTentativaEmAsc(
			StatusImagem status, Instant agora, Limit limite);

	/** Primeira foto registrada de um album; o anuncio dela e o do album inteiro. */
	Optional<ImagemAnuncio> findFirstByIdAlbumWhatsappOrderByIdAsc(String idAlbumWhatsapp);

	/** Todas as imagens do anuncio, em qualquer status (usada para a ordem e para saber se ja tem fotos). */
	long countByAnuncioId(Long anuncioId);

	/** Para o limite de imagens por anuncio. */
	long countByAnuncioIdAndStatusIn(Long anuncioId, Collection<StatusImagem> status);

	default List<ImagemAnuncio> pendentesProntasParaTentar(Instant agora, int limite) {
		return findByStatusAndProximaTentativaEmLessThanEqualOrderByProximaTentativaEmAsc(StatusImagem.PENDENTE,
				agora, Limit.of(limite));
	}
}
