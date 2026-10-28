package com.dev.isac_plus.model;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

/**
 * Uma imagem recebida pelo WhatsApp e ligada a um anuncio.
 * O status so muda pelos metodos de transicao (marcarSalva, rejeitar, registrarFalha).
 */
@Entity
@Table(name = "imagem_anuncio", indexes = {
		@Index(name = "idx_imagem_status_proxima", columnList = "status, proxima_tentativa_em"),
		@Index(name = "idx_imagem_album", columnList = "id_album_whatsapp") })
public class ImagemAnuncio {

	/** Espera antes de cada nova tentativa. Depois da ultima falha a imagem fica FALHOU. */
	static final List<Duration> ESPERAS = List.of(Duration.ofSeconds(30), Duration.ofMinutes(2),
			Duration.ofMinutes(10));

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "anuncio_id", nullable = false)
	private Anuncio anuncio;

	// Unico para nao registrar de novo quando a Evolution reenvia o evento.
	@Column(unique = true, nullable = false)
	private String idWhatsapp;

	private String idAlbumWhatsapp;

	// Necessaria para chamar a Evolution depois (getBase64FromMediaMessage/{instancia}).
	@Column(nullable = false)
	private String instancia;

	private String numero;

	private String chaveStorage;

	private String contentType;

	private Long tamanhoBytes;

	private Integer largura;

	private Integer altura;

	private int ordem;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private StatusImagem status;

	private int tentativas;

	// Nulo quando nao ha mais nada a fazer (SALVA, FALHOU, REJEITADA).
	private Instant proximaTentativaEm;

	@Column(columnDefinition = "text")
	private String erro;

	// O "data" do webhook, enviado de volta para a Evolution na hora do download.
	@Column(columnDefinition = "text", nullable = false)
	private String payloadEvolution;

	@Column(nullable = false, updatable = false)
	private Instant criadaEm;

	@Column(nullable = false)
	private Instant atualizadaEm;

	protected ImagemAnuncio() {
	}

	/** Cria a imagem como PENDENTE, pronta para o primeiro download. */
	public ImagemAnuncio(Anuncio anuncio, String idWhatsapp, String idAlbumWhatsapp, String instancia,
			String numero, MetadadosImagem metadados, int ordem, String payloadEvolution) {
		this.anuncio = anuncio;
		this.idWhatsapp = idWhatsapp;
		this.idAlbumWhatsapp = idAlbumWhatsapp;
		this.instancia = instancia;
		this.numero = numero;
		this.contentType = metadados.contentType();
		this.tamanhoBytes = metadados.tamanhoBytes();
		this.largura = metadados.largura();
		this.altura = metadados.altura();
		this.ordem = ordem;
		this.payloadEvolution = payloadEvolution;
		this.status = StatusImagem.PENDENTE;
		this.tentativas = 0;
		this.proximaTentativaEm = Instant.now();
	}

	public void marcarSalva(String chaveStorage, MetadadosImagem metadados) {
		exigirPendente();
		this.chaveStorage = chaveStorage;
		this.contentType = metadados.contentType();
		this.tamanhoBytes = metadados.tamanhoBytes();
		// A Evolution nem sempre devolve as dimensoes; mantem as do webhook nesse caso.
		if (metadados.largura() != null) {
			this.largura = metadados.largura();
		}
		if (metadados.altura() != null) {
			this.altura = metadados.altura();
		}
		this.status = StatusImagem.SALVA;
		this.proximaTentativaEm = null;
		this.erro = null;
	}

	public void rejeitar(String motivo) {
		exigirPendente();
		this.status = StatusImagem.REJEITADA;
		this.proximaTentativaEm = null;
		this.erro = motivo;
	}

	/**
	 * Conta uma falha passageira. Agenda a proxima tentativa com espera crescente
	 * ou, se as tentativas acabaram, marca como FALHOU.
	 */
	public void registrarFalha(String erro, Instant agora) {
		exigirPendente();
		this.erro = erro;
		if (tentativas < ESPERAS.size()) {
			this.proximaTentativaEm = agora.plus(ESPERAS.get(tentativas));
		} else {
			this.status = StatusImagem.FALHOU;
			this.proximaTentativaEm = null;
		}
		this.tentativas++;
	}

	private void exigirPendente() {
		if (status != StatusImagem.PENDENTE) {
			throw new IllegalStateException("Imagem " + id + " nao esta PENDENTE (status " + status + ")");
		}
	}

	@PrePersist
	void aoCriar() {
		criadaEm = Instant.now();
		atualizadaEm = criadaEm;
	}

	@PreUpdate
	void aoAtualizar() {
		atualizadaEm = Instant.now();
	}

	public Long getId() {
		return id;
	}

	public Anuncio getAnuncio() {
		return anuncio;
	}

	public String getIdWhatsapp() {
		return idWhatsapp;
	}

	public String getIdAlbumWhatsapp() {
		return idAlbumWhatsapp;
	}

	public String getInstancia() {
		return instancia;
	}

	public String getNumero() {
		return numero;
	}

	public String getChaveStorage() {
		return chaveStorage;
	}

	public String getContentType() {
		return contentType;
	}

	public Long getTamanhoBytes() {
		return tamanhoBytes;
	}

	public Integer getLargura() {
		return largura;
	}

	public Integer getAltura() {
		return altura;
	}

	public int getOrdem() {
		return ordem;
	}

	public StatusImagem getStatus() {
		return status;
	}

	public int getTentativas() {
		return tentativas;
	}

	public Instant getProximaTentativaEm() {
		return proximaTentativaEm;
	}

	public String getErro() {
		return erro;
	}

	public String getPayloadEvolution() {
		return payloadEvolution;
	}

	public Instant getCriadaEm() {
		return criadaEm;
	}

	public Instant getAtualizadaEm() {
		return atualizadaEm;
	}
}
