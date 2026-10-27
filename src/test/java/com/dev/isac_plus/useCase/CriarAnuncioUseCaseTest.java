package com.dev.isac_plus.useCase;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.dev.isac_plus.dto.EvolutionWebhookEvento;
import com.dev.isac_plus.dto.MensagemEnviadaDTO;
import com.dev.isac_plus.dto.MensagemRecebida;
import com.dev.isac_plus.restClient.EvolutionClient;
import com.dev.isac_plus.service.AnuncioService;
import com.dev.isac_plus.service.WebhookService;

import tools.jackson.databind.json.JsonMapper;

@ExtendWith(MockitoExtension.class)
class CriarAnuncioUseCaseTest {

	private static final String INSTANCIA = "ong";
	private static final String NUMERO = "5511999";

	@Mock
	private WebhookService webhookService;

	@Mock
	private AnuncioService anuncioService;

	@Mock
	private EvolutionClient evolutionClient;

	private CriarAnuncioUseCase useCase;
	private int idEvento;

	@BeforeEach
	void setUp() {
		useCase = new CriarAnuncioUseCase(webhookService, anuncioService, evolutionClient);
	}

	@Test
	void primeiraMensagemPerguntaSeQuerCriarAnuncioSemCriarUm() {
		MensagemRecebida mensagemInicial = mensagem("Oi, quero divulgar algo");
		processar(mensagemInicial);

		verify(anuncioService, never()).criarDeMensagem(org.mockito.ArgumentMatchers.any());
		verificarResposta(mensagemInicial, "Olá! 👋 Eu sou o Isac-plus, seu assistente para criação de anúncios.\n\nGostaria de criar um anúncio? Responda sim ou não para continuarmos.");
	}

	@Test
	void respostaSimPedeTextoEProximaMensagemCriaAnuncio() {
		MensagemRecebida inicial = mensagem("Olá");
		MensagemRecebida confirmacao = mensagem("  SIm  ");
		MensagemRecebida titulo = mensagem("Bicicleta em ótimo estado");
		MensagemRecebida textoAnuncio = mensagem("Vendo bicicleta em bom estado");
		processar(inicial);
		processar(confirmacao);
		processar(titulo);
		processar(textoAnuncio);

		verify(anuncioService).criarDeMensagem(textoAnuncio, "Bicicleta em ótimo estado", "Vendo bicicleta em bom estado");
		verificarResposta(confirmacao, "Que bom poder ajudar! 😊\n\nPor favor, informe o título que gostaria de usar no anúncio.");
		verificarResposta(titulo, "Obrigado pelo título!\n\nAgora, por gentileza, envie o texto com os detalhes que deseja publicar no anúncio.");
		verificarResposta(textoAnuncio, "Anúncio criado com sucesso! 🎉\n\nAgradeço por utilizar o Isac-plus. Se precisar de ajuda com outro anúncio, é só me chamar.");
	}

	@Test
	void respostaNaoEncerraSemCriarAnuncio() {
		MensagemRecebida inicial = mensagem("Oi");
		MensagemRecebida respostaNao = mensagem("NÃO");
		processar(inicial);
		processar(respostaNao);

		verify(anuncioService, never()).criarDeMensagem(org.mockito.ArgumentMatchers.any());
		verificarResposta(respostaNao,
				"Tudo bem, não criarei o anúncio. 😊\n\nQuando quiser começar, basta enviar uma nova mensagem. Estarei à disposição!");
	}

	@Test
	void conversaDeUmNumeroNaoAvancaEtapaDeOutroNumero() {
		MensagemRecebida primeiraPessoa = mensagem("5511001", "Oi");
		MensagemRecebida segundaPessoa = mensagem("5511002", "sim");
		processar(primeiraPessoa);
		processar(segundaPessoa);

		verificarResposta(segundaPessoa, "Olá! 👋 Eu sou o Isac-plus, seu assistente para criação de anúncios.\n\nGostaria de criar um anúncio? Responda sim ou não para continuarmos.");
		verify(anuncioService, never()).criarDeMensagem(org.mockito.ArgumentMatchers.any());
	}

	@Test
	void respostaInvalidaRepetePerguntaEContinuaAguardandoConfirmacao() {
		MensagemRecebida inicial = mensagem("Oi");
		MensagemRecebida respostaInvalida = mensagem("talvez");
		MensagemRecebida confirmacao = mensagem("sim");
		processar(inicial);
		processar(respostaInvalida);
		processar(confirmacao);

		verificarResposta(respostaInvalida, "Olá! 👋 Eu sou o Isac-plus, seu assistente para criação de anúncios.\n\nGostaria de criar um anúncio? Responda sim ou não para continuarmos.");
		verificarResposta(confirmacao, "Que bom poder ajudar! 😊\n\nPor favor, informe o título que gostaria de usar no anúncio.");
	}

	@Test
	void aposCriarAnuncioProximaMensagemIniciaNovaConversa() {
		MensagemRecebida inicial = mensagem("Oi");
		MensagemRecebida confirmacao = mensagem("sim");
		MensagemRecebida titulo = mensagem("Título");
		MensagemRecebida texto = mensagem("Anúncio");
		MensagemRecebida novaConversa = mensagem("Outro assunto");
		processar(inicial);
		processar(confirmacao);
		processar(titulo);
		processar(texto);
		processar(novaConversa);

		verificarResposta(novaConversa, "Olá! 👋 Eu sou o Isac-plus, seu assistente para criação de anúncios.\n\nGostaria de criar um anúncio? Responda sim ou não para continuarmos.");
	}

	@Test
	void anuncioNaoECriadoAteReceberTituloETexto() {
		MensagemRecebida inicial = mensagem("Oi");
		MensagemRecebida confirmacao = mensagem("sim");
		MensagemRecebida titulo = mensagem("Meu anúncio");
		processar(inicial);
		processar(confirmacao);
		processar(titulo);

		verify(anuncioService, never()).criarDeMensagem(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString());
	}

	@Test
	void eventoIgnoradoNaoEnviaRespostaNemCriaAnuncio() {
		EvolutionWebhookEvento evento = new EvolutionWebhookEvento("contacts.upsert", INSTANCIA,
				JsonMapper.builder().build().createObjectNode());
		when(webhookService.processar(evento)).thenReturn(Optional.empty());

		useCase.executar(evento);

		verify(anuncioService, never()).criarDeMensagem(org.mockito.ArgumentMatchers.any());
		verify(evolutionClient, never()).enviarMensagem(org.mockito.ArgumentMatchers.anyString(),
				org.mockito.ArgumentMatchers.any(MensagemEnviadaDTO.class));
	}

	private MensagemRecebida mensagem(String texto) {
		return mensagem(NUMERO, texto);
	}

	private MensagemRecebida mensagem(String numero, String texto) {
		return new MensagemRecebida("id-" + idEvento++, INSTANCIA, numero, "Ana", texto);
	}

	private void processar(MensagemRecebida mensagem) {
		var data = JsonMapper.builder().build().createObjectNode();
		data.put("testId", mensagem.idWhatsapp());
		EvolutionWebhookEvento evento = new EvolutionWebhookEvento("messages.upsert", mensagem.instancia(),
				data);
		when(webhookService.processar(evento)).thenReturn(Optional.of(mensagem));
		useCase.executar(evento);
	}

	private void verificarResposta(MensagemRecebida destinatario, String texto) {
		verify(evolutionClient).enviarMensagem(eq(destinatario.instancia()),
				eq(new MensagemEnviadaDTO(destinatario.numero(), texto)));
	}
}
