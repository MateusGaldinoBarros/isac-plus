package com.dev.isac_plus.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import com.dev.isac_plus.model.Anuncio;
import com.dev.isac_plus.model.ImagemAnuncio;
import com.dev.isac_plus.model.MetadadosImagem;
import com.dev.isac_plus.model.StatusImagem;

import jakarta.persistence.EntityManager;

/** Roda num H2 em memoria; o Neon nao e tocado. */
@DataJpaTest
class ImagemAnuncioRepositoryTest {

	@Autowired
	private ImagemAnuncioRepository repository;

	@Autowired
	private AnuncioRepository anuncioRepository;

	@Autowired
	private EntityManager em;

	private Anuncio anuncio;

	@BeforeEach
	void setUp() {
		anuncio = anuncioRepository.save(new Anuncio("", "Bike", "5511999"));
	}

	@Test
	void salvaELigaAoAnuncioNaOrdem() {
		repository.save(imagem("B", 1));
		repository.save(imagem("A", 0));
		em.flush();
		em.clear();

		Anuncio recarregado = anuncioRepository.findById(anuncio.getId()).orElseThrow();

		assertThat(recarregado.getImagens()).extracting(ImagemAnuncio::getIdWhatsapp).containsExactly("A", "B");
		assertThat(recarregado.getImagens().get(0).getCriadaEm()).isNotNull();
	}

	@Test
	void existsByIdWhatsappDetectaReenvio() {
		repository.save(imagem("MSG1", 0));

		assertThat(repository.existsByIdWhatsapp("MSG1")).isTrue();
		assertThat(repository.existsByIdWhatsapp("MSG2")).isFalse();
	}

	@Test
	void idWhatsappDuplicadoEhBarradoPeloBanco() {
		repository.saveAndFlush(imagem("MSG1", 0));

		assertThatThrownBy(() -> repository.saveAndFlush(imagem("MSG1", 1)))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void pendentesProntasIgnoraAsAgendadasParaDepoisEAsFinalizadas() {
		Instant agora = Instant.now();
		ImagemAnuncio pronta = imagem("PRONTA", 0);
		ImagemAnuncio agendada = imagem("AGENDADA", 1);
		agendada.registrarFalha("timeout", agora);
		ImagemAnuncio salva = imagem("SALVA", 2);
		salva.marcarSalva("k", new MetadadosImagem("image/jpeg", 1L, null, null));
		ImagemAnuncio rejeitada = imagem("REJEITADA", 3);
		rejeitada.rejeitar("gif");
		repository.saveAll(List.of(pronta, agendada, salva, rejeitada));

		assertThat(repository.pendentesProntasParaTentar(agora.plusSeconds(1), 10))
				.extracting(ImagemAnuncio::getIdWhatsapp).containsExactly("PRONTA");
		assertThat(repository.pendentesProntasParaTentar(agora.plus(Duration.ofMinutes(1)), 10))
				.extracting(ImagemAnuncio::getIdWhatsapp).containsExactly("PRONTA", "AGENDADA");
	}

	@Test
	void pendentesProntasRespeitaOLimite() {
		repository.saveAll(List.of(imagem("1", 0), imagem("2", 1), imagem("3", 2)));

		assertThat(repository.pendentesProntasParaTentar(Instant.now().plusSeconds(1), 2)).hasSize(2);
	}

	@Test
	void contaImagensDoAnuncioPorStatus() {
		ImagemAnuncio rejeitada = imagem("R", 2);
		rejeitada.rejeitar("gif");
		repository.saveAll(List.of(imagem("P1", 0), imagem("P2", 1), rejeitada));
		Anuncio outro = anuncioRepository.save(new Anuncio("", "Moto", "5511888"));
		repository.save(new ImagemAnuncio(outro, "OUTRO", null, "ong", "5511888",
				new MetadadosImagem("image/jpeg", 1L, null, null), 0, "{}"));

		assertThat(repository.countByAnuncioIdAndStatusIn(anuncio.getId(),
				List.of(StatusImagem.PENDENTE, StatusImagem.SALVA))).isEqualTo(2);
	}

	@Test
	void apagarOAnuncioApagaAsImagens() {
		repository.save(imagem("MSG1", 0));
		em.flush();
		em.clear();

		anuncioRepository.delete(anuncioRepository.findById(anuncio.getId()).orElseThrow());
		em.flush();

		assertThat(repository.count()).isZero();
	}

	private ImagemAnuncio imagem(String idWhatsapp, int ordem) {
		return new ImagemAnuncio(anuncio, idWhatsapp, null, "ong", "5511999",
				new MetadadosImagem("image/jpeg", 84512L, 1080, 1350), ordem, "{\"key\":{\"id\":\"" + idWhatsapp + "\"}}");
	}
}
