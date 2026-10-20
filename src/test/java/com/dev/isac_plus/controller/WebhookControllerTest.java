package com.dev.isac_plus.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.dev.isac_plus.dto.EvolutionWebhookEvento;
import com.dev.isac_plus.service.WebhookService;

@WebMvcTest(WebhookController.class)
class WebhookControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private WebhookService service;

	@Test
	void recebeEventoDaEvolutionERetorna200() throws Exception {
		String corpo = """
				{"event":"messages.upsert","instance":"ong","apikey":"x",
				 "data":{"key":{"remoteJid":"551188@s.whatsapp.net"},"message":{"conversation":"oi"}}}
				""";

		mockMvc.perform(post("/webhook/evolution").contentType(MediaType.APPLICATION_JSON).content(corpo))
				.andExpect(status().isOk());

		ArgumentCaptor<EvolutionWebhookEvento> captor = ArgumentCaptor.forClass(EvolutionWebhookEvento.class);
		verify(service).processar(captor.capture());
		assertThat(captor.getValue().event()).isEqualTo("messages.upsert");
		assertThat(captor.getValue().data().path("message").path("conversation").asString()).isEqualTo("oi");
	}

	@Test
	void eventoComDataEmListaTambemRetorna200() throws Exception {
		mockMvc.perform(post("/webhook/evolution").contentType(MediaType.APPLICATION_JSON)
				.content("{\"event\":\"contacts.upsert\",\"data\":[{\"id\":\"1\"}]}"))
				.andExpect(status().isOk());

		verify(service).processar(any());
	}
}
