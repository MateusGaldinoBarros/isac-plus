package com.dev.isac_plus.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.dev.isac_plus.dto.AnuncioRequest;
import com.dev.isac_plus.dto.AnuncioResponse;
import com.dev.isac_plus.exception.RecursoNaoEncontradoException;
import com.dev.isac_plus.service.AnuncioService;

@WebMvcTest(AnuncioController.class)
class AnuncioControllerTest {

	private static final String JSON_VALIDO = """
			{"titulo":"Bike","descricao":"Usada","contato":"11999990000"}
			""";

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private AnuncioService service;

	@Test
	void listarRetorna200ComOsAnuncios() throws Exception {
		when(service.listar()).thenReturn(List.of(new AnuncioResponse(1L, "Bike", "Usada", "11999990000")));

		mockMvc.perform(get("/anuncios"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].id").value(1))
				.andExpect(jsonPath("$[0].titulo").value("Bike"));
	}

	@Test
	void buscarPorIdInexistenteRetorna404() throws Exception {
		when(service.buscarPorId(99L)).thenThrow(new RecursoNaoEncontradoException("Anuncio", 99L));

		mockMvc.perform(get("/anuncios/99"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.mensagem").value("Anuncio com id 99 nao encontrado"));
	}

	@Test
	void criarRetorna201ComLocation() throws Exception {
		when(service.criar(any(AnuncioRequest.class))).thenReturn(new AnuncioResponse(5L, "Bike", "Usada", "11999990000"));

		mockMvc.perform(post("/anuncios").contentType(MediaType.APPLICATION_JSON).content(JSON_VALIDO))
				.andExpect(status().isCreated())
				.andExpect(header().string("Location", "/anuncios/5"))
				.andExpect(jsonPath("$.id").value(5));

		verify(service).criar(new AnuncioRequest("Bike", "Usada", "11999990000"));
	}

	@Test
	void criarComDadosInvalidosRetorna400ENaoChamaOService() throws Exception {
		mockMvc.perform(post("/anuncios").contentType(MediaType.APPLICATION_JSON).content("{\"titulo\":\"\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.campos.titulo").exists())
				.andExpect(jsonPath("$.campos.contato").exists());

		verifyNoInteractions(service);
	}

	@Test
	void atualizarRetorna200() throws Exception {
		when(service.atualizar(eq(1L), any(AnuncioRequest.class)))
				.thenReturn(new AnuncioResponse(1L, "Bike", "Usada", "11999990000"));

		mockMvc.perform(put("/anuncios/1").contentType(MediaType.APPLICATION_JSON).content(JSON_VALIDO))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.titulo").value("Bike"));
	}

	@Test
	void excluirRetorna204() throws Exception {
		mockMvc.perform(delete("/anuncios/1")).andExpect(status().isNoContent());

		verify(service).excluir(1L);
	}

	@Test
	void excluirInexistenteRetorna404() throws Exception {
		doThrow(new RecursoNaoEncontradoException("Anuncio", 99L)).when(service).excluir(99L);

		mockMvc.perform(delete("/anuncios/99")).andExpect(status().isNotFound());
	}
}
