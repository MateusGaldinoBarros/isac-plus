package com.dev.isac_plus.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;

import com.dev.isac_plus.client.EvolutionClient;
import com.dev.isac_plus.dto.MidiaBaixada;
import com.dev.isac_plus.model.Anuncio;
import com.dev.isac_plus.model.ImagemAnuncio;
import com.dev.isac_plus.model.MetadadosImagem;
import com.dev.isac_plus.model.StatusImagem;
import com.dev.isac_plus.repository.ImagemAnuncioRepository;

import tools.jackson.databind.json.JsonMapper;

/** O arquivo subiu para o storage, mas gravar SALVA no banco falhou. */
@ExtendWith(MockitoExtension.class)
class ProcessadorImagensFalhaNoBancoTest {

	private static final byte[] JPEG = { (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x01 };

	@Mock
	private ImagemAnuncioRepository repository;

	@Mock
	private EvolutionClient evolution;

	@Mock
	private StorageService storage;

	@Test
	void apagaOArquivoEnviadoERegistraAFalhaNaVersaoDoBanco() {
		ImagemAnuncio emMemoria = pendente();
		ImagemAnuncio doBanco = pendente();
		when(evolution.baixarMidia(anyString(), any())).thenReturn(new MidiaBaixada(JPEG, null, null, null, null));
		when(repository.save(emMemoria)).thenThrow(new DataAccessResourceFailureException("banco caiu"));
		when(repository.findById(any())).thenReturn(Optional.of(doBanco));
		ProcessadorImagens processador = new ProcessadorImagens(repository, evolution, storage, new ValidadorImagem(),
				Clock.systemUTC(), JsonMapper.builder().build());

		processador.processar(emMemoria);

		ArgumentCaptor<String> chave = ArgumentCaptor.forClass(String.class);
		verify(storage).enviar(chave.capture(), any(), anyString());
		verify(storage).apagar(chave.getValue());
		verify(repository).save(doBanco);
		assertThat(doBanco.getStatus()).isEqualTo(StatusImagem.PENDENTE);
		assertThat(doBanco.getTentativas()).isEqualTo(1);
		assertThat(doBanco.getErro()).contains("banco caiu");
	}

	private static ImagemAnuncio pendente() {
		return new ImagemAnuncio(new Anuncio("", "Bike", "5511999"), "IMG1", null, "ong", "5511999",
				new MetadadosImagem("image/jpeg", 1L, null, null), 0, "{\"key\":{\"id\":\"IMG1\"}}");
	}
}
