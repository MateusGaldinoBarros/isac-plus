package com.dev.isac_plus.service;

import java.time.Clock;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.dev.isac_plus.client.EvolutionClient;
import com.dev.isac_plus.dto.MidiaBaixada;
import com.dev.isac_plus.exception.ImagemInvalidaException;
import com.dev.isac_plus.model.ImagemAnuncio;
import com.dev.isac_plus.model.MetadadosImagem;
import com.dev.isac_plus.repository.ImagemAnuncioRepository;

import tools.jackson.databind.json.JsonMapper;

/**
 * Pega as imagens PENDENTE cuja hora chegou, baixa da Evolution, confere e guarda no storage.
 *
 * As chamadas de rede ficam fora de transacao (cada save e curto), para nao prender uma conexao
 * do banco enquanto espera a Evolution. Roda numa thread so, entao uma imagem nunca e processada
 * duas vezes ao mesmo tempo.
 */
@Component
public class ProcessadorImagens {

	static final int LOTE = 20;

	private static final Logger log = LoggerFactory.getLogger(ProcessadorImagens.class);

	private final ImagemAnuncioRepository repository;
	private final EvolutionClient evolution;
	private final StorageService storage;
	private final ValidadorImagem validador;
	private final Clock relogio;
	private final JsonMapper json;

	public ProcessadorImagens(ImagemAnuncioRepository repository, EvolutionClient evolution, StorageService storage,
			ValidadorImagem validador, Clock relogio, JsonMapper json) {
		this.repository = repository;
		this.evolution = evolution;
		this.storage = storage;
		this.validador = validador;
		this.relogio = relogio;
		this.json = json;
	}

	@Scheduled(initialDelayString = "${imagens.processamento.atraso-inicial:10s}",
			fixedDelayString = "${imagens.processamento.intervalo:10s}")
	public void processarPendentes() {
		List<ImagemAnuncio> pendentes;
		try {
			pendentes = repository.pendentesProntasParaTentar(relogio.instant(), LOTE);
		} catch (RuntimeException e) {
			log.warn("Nao foi possivel buscar imagens pendentes: {}", e.getMessage());
			return;
		}
		pendentes.forEach(this::processar);
	}

	void processar(ImagemAnuncio imagem) {
		String chave = null;
		try {
			MidiaBaixada midia = evolution.baixarMidia(imagem.getInstancia(),
					json.readTree(imagem.getPayloadEvolution()));
			// O tipo real vem dos bytes; o mimetype informado pode estar errado
			String tipo = validador.validarConteudo(midia.conteudo());
			chave = "anuncios/" + imagem.getAnuncio().getId() + "/" + UUID.randomUUID() + extensao(tipo);
			storage.enviar(chave, midia.conteudo(), tipo);

			imagem.marcarSalva(chave, new MetadadosImagem(tipo, (long) midia.conteudo().length, midia.largura(),
					midia.altura()));
			repository.save(imagem);
			log.info("Imagem {} salva em {}", imagem.getId(), chave);
		} catch (ImagemInvalidaException e) {
			log.info("Imagem {} rejeitada: {}", imagem.getId(), e.getMessage());
			atualizar(imagem, i -> i.rejeitar(e.getMessage()));
		} catch (RuntimeException e) {
			if (chave != null) {
				apagarSemFalhar(chave);
			}
			log.warn("Falha ao processar imagem {}: {}", imagem.getId(), e.getMessage());
			atualizar(imagem, i -> i.registrarFalha(resumir(e), relogio.instant()));
		}
	}

	/**
	 * Relê a imagem do banco antes de mudar o status: o objeto em memoria pode ja estar
	 * marcado como SALVA se o erro foi justamente no save.
	 */
	private void atualizar(ImagemAnuncio imagem, Consumer<ImagemAnuncio> mudanca) {
		try {
			repository.findById(imagem.getId()).ifPresent(atual -> {
				mudanca.accept(atual);
				repository.save(atual);
			});
		} catch (RuntimeException e) {
			// Continua PENDENTE no banco e sera tentada de novo no proximo ciclo
			log.warn("Nao foi possivel atualizar a imagem {}: {}", imagem.getId(), e.getMessage());
		}
	}

	private void apagarSemFalhar(String chave) {
		try {
			storage.apagar(chave);
		} catch (RuntimeException e) {
			log.warn("Arquivo {} ficou orfao no storage: {}", chave, e.getMessage());
		}
	}

	private static String resumir(RuntimeException e) {
		String mensagem = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
		return mensagem.length() > 1000 ? mensagem.substring(0, 1000) : mensagem;
	}

	static String extensao(String tipo) {
		return switch (tipo) {
			case "image/png" -> ".png";
			case "image/webp" -> ".webp";
			default -> ".jpg";
		};
	}
}
