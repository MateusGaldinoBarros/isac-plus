package com.dev.isac_plus.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.dev.isac_plus.dto.ArquivoImagem;
import com.dev.isac_plus.exception.RecursoNaoEncontradoException;
import com.dev.isac_plus.exception.StorageException;
import com.dev.isac_plus.service.ImagemService;

@WebMvcTest(ImagemController.class)
class ImagemControllerTest {

	private static final byte[] JPEG = { (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x01 };

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private ImagemService service;

	@Test
	void devolveOArquivoComTipoTamanhoECacheLongo() throws Exception {
		when(service.buscarArquivo(7L)).thenReturn(new ArquivoImagem(JPEG, "image/jpeg"));

		mockMvc.perform(get("/imagens/7"))
				.andExpect(status().isOk())
				.andExpect(header().string("Content-Type", "image/jpeg"))
				.andExpect(header().longValue("Content-Length", JPEG.length))
				.andExpect(header().string("Cache-Control", "max-age=31536000, public, immutable"))
				.andExpect(header().string("X-Content-Type-Options", "nosniff"))
				.andExpect(content().bytes(JPEG));
	}

	@Test
	void imagemInexistenteResponde404() throws Exception {
		when(service.buscarArquivo(99L)).thenThrow(new RecursoNaoEncontradoException("Imagem", 99L));

		mockMvc.perform(get("/imagens/99"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.mensagem").value("Imagem com id 99 nao encontrado"));
	}

	@Test
	void storageForaDoArResponde503SemCache() throws Exception {
		when(service.buscarArquivo(7L)).thenThrow(new StorageException("fora", null));

		mockMvc.perform(get("/imagens/7"))
				.andExpect(status().isServiceUnavailable())
				.andExpect(header().doesNotExist("Cache-Control"))
				.andExpect(jsonPath("$.status").value(503));
	}
}
