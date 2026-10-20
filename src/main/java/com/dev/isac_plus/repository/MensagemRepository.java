package com.dev.isac_plus.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.dev.isac_plus.model.Mensagem;

public interface MensagemRepository extends JpaRepository<Mensagem, Long> {

	boolean existsByIdWhatsapp(String idWhatsapp);
}
