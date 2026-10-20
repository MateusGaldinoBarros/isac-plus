package com.dev.isac_plus.model;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class Mensagem {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	// Unico para nao salvar de novo quando a Evolution reenvia o evento.
	@Column(unique = true)
	private String idWhatsapp;

	private String instancia;

	private String numero;

	private String nome;

	@Column(columnDefinition = "text")
	private String texto;

	private Instant recebidaEm;

	protected Mensagem() {
	}

	public Mensagem(String idWhatsapp, String instancia, String numero, String nome, String texto, Instant recebidaEm) {
		this.idWhatsapp = idWhatsapp;
		this.instancia = instancia;
		this.numero = numero;
		this.nome = nome;
		this.texto = texto;
		this.recebidaEm = recebidaEm;
	}

	public Long getId() {
		return id;
	}

	public String getIdWhatsapp() {
		return idWhatsapp;
	}

	public String getInstancia() {
		return instancia;
	}

	public String getNumero() {
		return numero;
	}

	public String getNome() {
		return nome;
	}

	public String getTexto() {
		return texto;
	}

	public Instant getRecebidaEm() {
		return recebidaEm;
	}
}
