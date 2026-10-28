package com.dev.isac_plus.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hibernate.annotations.BatchSize;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;

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

	// Anuncio que chegou so com fotos; fica fora da listagem ate a pessoa mandar o texto.
	// O default preenche as linhas que ja existiam quando a coluna foi criada.
	@Column(columnDefinition = "boolean default false not null")
	private boolean rascunho;

	// Nulo nos anuncios criados antes desta coluna existir.
	private Instant criadoEm;

	// Apagar o anuncio apaga as linhas das imagens (sem isso a chave estrangeira impediria o delete).
	@OneToMany(mappedBy = "anuncio", cascade = CascadeType.REMOVE)
	@OrderBy("ordem ASC, id ASC")
	// Na listagem carrega as imagens de ate 50 anuncios por consulta, em vez de uma consulta por anuncio
	@BatchSize(size = 50)
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

	/** Anuncio sem descricao, criado quando chegam fotos sem texto. */
	public static Anuncio rascunho(String contato) {
		Anuncio anuncio = new Anuncio("", null, contato, null);
		anuncio.rascunho = true;
		return anuncio;
	}

	/** O texto chegou depois das fotos: vira a descricao e o anuncio sai do rascunho. */
	public void publicarComDescricao(String descricao, String idWhatsapp) {
		this.descricao = descricao;
		if (this.idWhatsapp == null) {
			this.idWhatsapp = idWhatsapp;
		}
		this.rascunho = false;
	}

	@PrePersist
	void aoCriar() {
		if (criadoEm == null) {
			criadoEm = Instant.now();
		}
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

	public boolean isRascunho() {
		return rascunho;
	}

	public Instant getCriadoEm() {
		return criadoEm;
	}

	/** Somente leitura: imagens sao criadas pelo fluxo do webhook, nao pelo anuncio. */
	public List<ImagemAnuncio> getImagens() {
		return Collections.unmodifiableList(imagens);
	}
}
