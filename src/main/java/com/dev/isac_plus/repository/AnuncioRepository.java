package com.dev.isac_plus.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.dev.isac_plus.model.Anuncio;

public interface AnuncioRepository extends JpaRepository<Anuncio, Long> {

	boolean existsByIdWhatsapp(String idWhatsapp);

	List<Anuncio> findByRascunhoFalse();

	Optional<Anuncio> findByIdAndRascunhoFalse(Long id);

	Optional<Anuncio> findFirstByContatoAndCriadoEmGreaterThanEqualOrderByCriadoEmDescIdDesc(String contato,
			Instant desde);

	/** Anuncio mais recente do numero criado a partir de {@code desde}. */
	default Optional<Anuncio> maisRecenteDoNumeroDesde(String contato, Instant desde) {
		return findFirstByContatoAndCriadoEmGreaterThanEqualOrderByCriadoEmDescIdDesc(contato, desde);
	}
}
