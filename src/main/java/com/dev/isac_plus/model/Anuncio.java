package com.dev.isac_plus.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;

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

	// Apagar o anuncio apaga as linhas das imagens (sem isso a chave estrangeira impediria o delete).
	@OneToMany(mappedBy = "anuncio", cascade = CascadeType.REMOVE)
	@OrderBy("ordem ASC, id ASC")
	private List<ImagemAnuncio> imagens = new ArrayList<>();

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

	/** Somente leitura: imagens sao criadas pelo fluxo do webhook, nao pelo anuncio. */
	public List<ImagemAnuncio> getImagens() {
		return Collections.unmodifiableList(imagens);
	}
}
