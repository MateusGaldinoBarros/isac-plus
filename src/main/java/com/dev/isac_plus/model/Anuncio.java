package com.dev.isac_plus.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class Anuncio {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String titulo;

	private String descricao;

	private String contato;

	@Column(unique = true)
	private String idWhatsapp;

	protected Anuncio() {
	}

	public Anuncio(String titulo, String descricao, String contato) {
		this(titulo, descricao, contato, null);
	}

	public Anuncio(String titulo, String descricao, String contato, String idWhatsapp) {
		this.titulo = titulo;
		this.descricao = descricao;
		this.contato = contato;
		this.idWhatsapp = idWhatsapp;
	}

	public Long getId() {
		return id;
	}

	public String getTitulo() {
		return titulo;
	}

	public void setTitulo(String titulo) {
		this.titulo = titulo;
	}

	public String getDescricao() {
		return descricao;
	}

	public void setDescricao(String descricao) {
		this.descricao = descricao;
	}

	public String getContato() {
		return contato;
	}

	public String getIdWhatsapp() {
		return idWhatsapp;
	}

	public void setContato(String contato) {
		this.contato = contato;
	}
}
