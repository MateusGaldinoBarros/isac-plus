package com.dev.isac_plus.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.dev.isac_plus.model.Anuncio;

public interface AnuncioRepository extends JpaRepository<Anuncio, Long> {
}
