package com.dev.isac_plus.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.dev.isac_plus.dto.EvolutionWebhookEvento;
import com.dev.isac_plus.dto.MensagemRecebida;
import com.dev.isac_plus.model.Mensagem;
import com.dev.isac_plus.repository.MensagemRepository;

import tools.jackson.databind.json.JsonMapper;

@ExtendWith(MockitoExtension.class)
class WebhookServiceTest {

	@Mock
	private MensagemRepository repository;

	private WebhookService service;
	private final JsonMapper json = JsonMapper.builder().build();

	@BeforeEach
	void setUp() {
		service = new WebhookService(repository);
	}

	private Optional<MensagemRecebida> processar(String evento, String data) {
		return service.processar(new EvolutionWebhookEvento(evento, "ong", json.readTree(data)));
	}

	@Test
	void extraiESalvaMensagemDeTextoSimples() {
		Optional<MensagemRecebida> resultado = processar("messages.upsert", """
				{"key":{"remoteJid":"5511999990000@s.whatsapp.net","fromMe":false,"id":"ABC123"},
				 "pushName":"Ana","message":{"conversation":"Oi, a bike ainda esta disponivel?"}}
				""");

		assertThat(resultado).contains(
				new MensagemRecebida("ABC123", "ong", "5511999990000", "Ana", "Oi, a bike ainda esta disponivel?"));

		ArgumentCaptor<Mensagem> captor = ArgumentCaptor.forClass(Mensagem.class);
		verify(repository).save(captor.capture());
		Mensagem salva = captor.getValue();
		assertThat(salva.getIdWhatsapp()).isEqualTo("ABC123");
		assertThat(salva.getNumero()).isEqualTo("5511999990000");
		assertThat(salva.getNome()).isEqualTo("Ana");
		assertThat(salva.getTexto()).isEqualTo("Oi, a bike ainda esta disponivel?");
		assertThat(salva.getRecebidaEm()).isNotNull();
	}

	@Test
	void naoSalvaDeNovoQuandoAEvolutionReenvia() {
		when(repository.existsByIdWhatsapp("ABC123")).thenReturn(true);

		processar("messages.upsert", """
				{"key":{"remoteJid":"551188@s.whatsapp.net","id":"ABC123"},"message":{"conversation":"oi"}}
				""");

		verify(repository, never()).save(any());
	}

	@Test
	void extraiTextoEstendidoELegendaDeImagem() {
		assertThat(processar("messages.upsert", """
				{"key":{"remoteJid":"551188@s.whatsapp.net"},"message":{"extendedTextMessage":{"text":"link aqui"}}}
				""")).map(MensagemRecebida::texto).contains("link aqui");

		assertThat(processar("messages.upsert", """
				{"key":{"remoteJid":"551188@s.whatsapp.net"},"message":{"imageMessage":{"caption":"foto da bike"}}}
				""")).map(MensagemRecebida::texto).contains("foto da bike");
	}

	@Test
	void usaSenderPnQuandoOContatoVemComLid() {
		assertThat(processar("messages.upsert", """
				{"key":{"remoteJid":"123456@lid","senderPn":"5511977770000@s.whatsapp.net"},
				 "message":{"conversation":"oi"}}
				""")).map(MensagemRecebida::numero).contains("5511977770000");
	}

	@Test
	void ignoraMensagensEnviadasPorNos() {
		assertThat(processar("messages.upsert", """
				{"key":{"remoteJid":"551188@s.whatsapp.net","fromMe":true},"message":{"conversation":"oi"}}
				""")).isEmpty();
	}

	@Test
	void ignoraGruposEStatus() {
		assertThat(processar("messages.upsert", """
				{"key":{"remoteJid":"1203630@g.us"},"message":{"conversation":"oi"}}
				""")).isEmpty();
		assertThat(processar("messages.upsert", """
				{"key":{"remoteJid":"status@broadcast"},"message":{"conversation":"oi"}}
				""")).isEmpty();
	}

	@Test
	void ignoraMensagemSemTexto() {
		assertThat(processar("messages.upsert", """
				{"key":{"remoteJid":"551188@s.whatsapp.net"},"message":{"audioMessage":{"seconds":3}}}
				""")).isEmpty();
	}

	@Test
	void ignoraOutrosEventosSemSalvarNada() {
		assertThat(processar("connection.update", "{\"state\":\"open\"}")).isEmpty();
		assertThat(service.processar(null)).isEmpty();

		verify(repository, never()).save(any());
	}
}
