package com.dev.isac_plus.useCase;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionOperations;

import com.dev.isac_plus.chatBot.ContextoConversa;
import com.dev.isac_plus.chatBot.EstadoInicial;
import com.dev.isac_plus.dto.EvolutionWebhookEvento;
import com.dev.isac_plus.dto.ImagemRecebida;
import com.dev.isac_plus.dto.MensagemRecebida;
import com.dev.isac_plus.model.MetadadosImagem;
import com.dev.isac_plus.service.RegistroImagemService;
import com.dev.isac_plus.service.TravaPorNumero;
import com.dev.isac_plus.service.WebhookService;

import tools.jackson.databind.json.JsonMapper;

@ExtendWith(MockitoExtension.class)
class CriarAnuncioUseCaseTest {

	private static final String INSTANCIA = "ong";
	private static final String NUMERO = "5511999";

	@Mock
	private WebhookService webhookService;

	@Mock
	private RegistroImagemService registroImagemService;

	@Mock
	private EstadoInicial estadoInicial;

	private CriarAnuncioUseCase useCase;

	@BeforeEach
	void setUp() {
		useCase = new CriarAnuncioUseCase(webhookService, registroImagemService,
				new TravaPorNumero(), TransactionOperations.withoutTransaction(), estadoInicial);
	}

	@Test
	void eventoSemRemetenteEhIgnorado() {
		EvolutionWebhookEvento evento = evento();
		when(webhookService.numeroDoRemetente(evento)).thenReturn(Optional.empty());

		useCase.executar(evento);

		verifyNoInteractions(registroImagemService, estadoInicial);
		verify(webhookService, never()).extrairImagem(evento);
		verify(webhookService, never()).processar(evento);
	}

	@Test
	void imagemEhRegistradaSemPassarPeloFluxoDeMensagem() {
		EvolutionWebhookEvento evento = evento();
		ImagemRecebida imagem = new ImagemRecebida("IMG", INSTANCIA, NUMERO, "Vendo bike", null,
				new MetadadosImagem("image/jpeg", 1L, null, null), "{}");
		when(webhookService.numeroDoRemetente(evento)).thenReturn(Optional.of(NUMERO));
		when(webhookService.extrairImagem(evento)).thenReturn(Optional.of(imagem));

		useCase.executar(evento);

		verify(registroImagemService).registrar(imagem);
		verify(webhookService, never()).processar(evento);
		verifyNoInteractions(estadoInicial);
	}

	@Test
	void mensagemDeTextoEhEnviadaAoEstadoInicial() {
		EvolutionWebhookEvento evento = evento();
		MensagemRecebida mensagem = new MensagemRecebida("msg", INSTANCIA, NUMERO, "Ana", "Oi");
		when(webhookService.numeroDoRemetente(evento)).thenReturn(Optional.of(NUMERO));
		when(webhookService.extrairImagem(evento)).thenReturn(Optional.empty());
		when(webhookService.processar(evento)).thenReturn(Optional.of(mensagem));

		useCase.executar(evento);

		verify(estadoInicial).processarMensagem(eq(mensagem), any(ContextoConversa.class));
		verifyNoInteractions(registroImagemService);
	}

	@Test
	void mensagensDoMesmoContatoCompartilhamContextoEMensagensDeContatosDiferentesNao() {
		MensagemRecebida primeira = new MensagemRecebida("1", INSTANCIA, NUMERO, "Ana", "Oi");
		MensagemRecebida mesmaPessoa = new MensagemRecebida("2", INSTANCIA, NUMERO, "Ana", "sim");
		MensagemRecebida outraPessoa = new MensagemRecebida("3", INSTANCIA, "5511888", "Bia", "Oi");
		processar(primeira);
		processar(mesmaPessoa);
		processar(outraPessoa);

		ArgumentCaptor<ContextoConversa> contextos = ArgumentCaptor.forClass(ContextoConversa.class);
		verify(estadoInicial, times(3)).processarMensagem(any(), contextos.capture());
		assertThat(contextos.getAllValues().get(0))
				.isSameAs(contextos.getAllValues().get(1))
				.isNotSameAs(contextos.getAllValues().get(2));
	}

	private void processar(MensagemRecebida mensagem) {
		EvolutionWebhookEvento evento = evento();
		when(webhookService.numeroDoRemetente(evento)).thenReturn(Optional.of(mensagem.numero()));
		when(webhookService.extrairImagem(evento)).thenReturn(Optional.empty());
		when(webhookService.processar(evento)).thenReturn(Optional.of(mensagem));
		useCase.executar(evento);
	}

	private EvolutionWebhookEvento evento() {
		return new EvolutionWebhookEvento("messages.upsert", INSTANCIA,
				JsonMapper.builder().build().createObjectNode());
	}
}
