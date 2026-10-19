package com.dev.isac_plus.exception;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.dev.isac_plus.dto.AnuncioRequest;

import jakarta.validation.Valid;

class GlobalExceptionHandlerTest {

	@RestController
	static class ControllerDeTeste {

		@GetMapping("/teste/{id}")
		String buscar(@PathVariable Long id) {
			if (id == 1) {
				throw new RecursoNaoEncontradoException("Anuncio", id);
			}
			throw new IllegalStateException("detalhe interno");
		}

		@PostMapping("/teste")
		String criar(@Valid @RequestBody AnuncioRequest request) {
			return "ok";
		}
	}

	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		mockMvc = MockMvcBuilders.standaloneSetup(new ControllerDeTeste())
				.setControllerAdvice(new GlobalExceptionHandler())
				.build();
	}

	@Test
	void recursoNaoEncontradoRetorna404() throws Exception {
		mockMvc.perform(get("/teste/1"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.status").value(404))
				.andExpect(jsonPath("$.mensagem").value("Anuncio com id 1 nao encontrado"));
	}

	@Test
	void validacaoRetorna400ComOsCampos() throws Exception {
		mockMvc.perform(post("/teste").contentType(MediaType.APPLICATION_JSON)
				.content("{\"titulo\":\"\",\"descricao\":\"x\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.mensagem").value("Dados invalidos"))
				.andExpect(jsonPath("$.campos.titulo").exists())
				.andExpect(jsonPath("$.campos.contato").exists());
	}

	@Test
	void jsonMalformadoRetorna400() throws Exception {
		mockMvc.perform(post("/teste").contentType(MediaType.APPLICATION_JSON).content("{nao e json"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.mensagem").value("Corpo da requisicao invalido"));
	}

	@Test
	void erroInesperadoRetorna500SemVazarDetalhes() throws Exception {
		mockMvc.perform(get("/teste/2"))
				.andExpect(status().isInternalServerError())
				.andExpect(jsonPath("$.mensagem").value("Erro interno no servidor"));
	}
}
