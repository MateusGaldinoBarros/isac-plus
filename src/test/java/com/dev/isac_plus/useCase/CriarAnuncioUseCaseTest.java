package com.dev.isac_plus.useCase;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionOperations;

import com.dev.isac_plus.dto.EvolutionWebhookEvento;
import com.dev.isac_plus.dto.ImagemRecebida;
import com.dev.isac_plus.dto.MensagemEnviadaDTO;
import com.dev.isac_plus.dto.MensagemRecebida;
import com.dev.isac_plus.model.MetadadosImagem;
import com.dev.isac_plus.restClient.EvolutionRestClient;
import com.dev.isac_plus.service.AnuncioService;
import com.dev.isac_plus.service.RegistroImagemService;
import com.dev.isac_plus.service.TravaPorNumero;
import com.dev.isac_plus.service.WebhookService;

import tools.jackson.databind.json.JsonMapper;

@ExtendWith(MockitoExtension.class)
class CriarAnuncioUseCaseTest {

	private static final String INSTANCIA = "ong";
	private static final String NUMERO = "5511999";
	private static final String PERGUNTA_CONFIRMACAO = "Olá! 👋 Eu sou o Isac-plus, seu assistente para criação de anúncios.\n\nGostaria de criar um anúncio? Responda sim ou não para continuarmos.";

	@Mock
	private WebhookService webhookService;

	@Mock
	private AnuncioService anuncioService;

	@Mock
	private EvolutionRestClient evolutionClient;

	@Mock
	private RegistroImagemService registroImagemService;

	private CriarAnuncioUseCase useCase;
	private int idEvento;

	@BeforeEach
	void setUp() {
		useCase = new CriarAnuncioUseCase(webhookService, anuncioService, evolutionClient,
				registroImagemService, new TravaPorNumero(), TransactionOperations.withoutTransaction());
	}

	@Test
	void primeiraMensagemPerguntaSeQuerCriarAnuncioSemCriarUm() {
		MensagemRecebida inicial = mensagem("Oi, quero divulgar algo");
		processar(inicial);

		verificarResposta(inicial, PERGUNTA_CONFIRMACAO);
		verify(anuncioService, never()).criarDeMensagem(any());
	}

	@Test
	void respostaSimPedeTituloETextoAntesDeCriarAnuncio() {
		MensagemRecebida inicial = mensagem("Olá");
		MensagemRecebida confirmacao = mensagem("  SIm  ");
		MensagemRecebida titulo = mensagem("Bicicleta em ótimo estado");
		MensagemRecebida texto = mensagem("Vendo bicicleta em bom estado");
		processar(inicial);
		processar(confirmacao);
		processar(titulo);
		processar(texto);

		verify(anuncioService).criarDeMensagem(texto, "Bicicleta em ótimo estado", "Vendo bicicleta em bom estado");
		verificarResposta(confirmacao, "Que bom poder ajudar! 😊\n\nPor favor, informe o título que gostaria de usar no anúncio.");
		verificarResposta(titulo, "Obrigado pelo título!\n\nAgora, por gentileza, envie o texto com os detalhes que deseja publicar no anúncio.");
		verificarResposta(texto, "Anúncio criado com sucesso! 🎉\n\nAgradeço por utilizar o Isac-plus. Se precisar de ajuda com outro anúncio, é só me chamar.");
	}

	@Test
	void respostaNaoEncerraSemCriarAnuncio() {
		MensagemRecebida inicial = mensagem("Oi");
		MensagemRecebida respostaNao = mensagem("NÃO");
		processar(inicial);
		processar(respostaNao);

		verify(anuncioService, never()).criarDeMensagem(any());
		verificarResposta(respostaNao, "Tudo bem, não criarei o anúncio. 😊\n\nQuando quiser começar, basta enviar uma nova mensagem. Estarei à disposição!");
	}

	@Test
	void conversaDeUmNumeroNaoAvancaEtapaDeOutroNumero() {
		MensagemRecebida primeiraPessoa = mensagem("5511001", "Oi");
		MensagemRecebida segundaPessoa = mensagem("5511002", "sim");
		processar(primeiraPessoa);
		processar(segundaPessoa);

		verificarResposta(primeiraPessoa, PERGUNTA_CONFIRMACAO);
		verificarResposta(segundaPessoa, PERGUNTA_CONFIRMACAO);
		verify(anuncioService, never()).criarDeMensagem(any());
	}

	@Test
	void respostaInvalidaRepetePerguntaEContinuaAguardandoConfirmacao() {
		MensagemRecebida inicial = mensagem("Oi");
		MensagemRecebida respostaInvalida = mensagem("talvez");
		MensagemRecebida confirmacao = mensagem("sim");
		processar(inicial);
		processar(respostaInvalida);
		processar(confirmacao);

		verificarResposta(respostaInvalida, PERGUNTA_CONFIRMACAO);
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

		verificarResposta(novaConversa, PERGUNTA_CONFIRMACAO);
	}

	@Test
	void anuncioNaoECriadoAteReceberTituloETexto() {
		processar(mensagem("Oi"));
		processar(mensagem("sim"));
		processar(mensagem("Meu anúncio"));

		verify(anuncioService, never()).criarDeMensagem(any(), anyString(), anyString());
	}

	@Test
	void eventoIgnoradoNaoEnviaRespostaNemCriaAnuncio() {
		EvolutionWebhookEvento evento = evento("contacts.upsert", INSTANCIA);
		when(webhookService.numeroDoRemetente(evento)).thenReturn(Optional.empty());

		useCase.executar(evento);

		verify(anuncioService, never()).criarDeMensagem(any());
		verify(evolutionClient, never()).enviarMensagem(anyString(), any(MensagemEnviadaDTO.class));
		verify(registroImagemService, never()).registrar(any());
	}

	@Test
	void imagemComLegendaVaiParaRegistroDeImagemSemCriarAnuncioDeTextoSeparado() {
		EvolutionWebhookEvento evento = evento("messages.upsert", INSTANCIA);
		ImagemRecebida imagem = new ImagemRecebida("IMG", INSTANCIA, NUMERO, "Vendo bike", null,
				new MetadadosImagem("image/jpeg", 1L, null, null), "{}");
		when(webhookService.numeroDoRemetente(evento)).thenReturn(Optional.of(NUMERO));
		when(webhookService.extrairImagem(evento)).thenReturn(Optional.of(imagem));

		useCase.executar(evento);

		verify(registroImagemService).registrar(imagem);
		verify(anuncioService, never()).criarDeMensagem(any());
		verify(evolutionClient, never()).enviarMensagem(anyString(), any(MensagemEnviadaDTO.class));
	}

	@Test
	void mensagemIgnoradaNaoCriaAnuncioNemRegistraImagem() {
		EvolutionWebhookEvento evento = evento("messages.upsert", INSTANCIA);
		when(webhookService.numeroDoRemetente(evento)).thenReturn(Optional.of(NUMERO));
		when(webhookService.extrairImagem(evento)).thenReturn(Optional.empty());
		when(webhookService.processar(evento)).thenReturn(Optional.empty());

		useCase.executar(evento);

		verify(anuncioService, never()).criarDeMensagem(any());
		verify(registroImagemService, never()).registrar(any());
		verify(evolutionClient, never()).enviarMensagem(anyString(), any(MensagemEnviadaDTO.class));
	}

	private MensagemRecebida mensagem(String texto) {
		return mensagem(NUMERO, texto);
	}

	private MensagemRecebida mensagem(String numero, String texto) {
		return new MensagemRecebida("id-" + idEvento++, INSTANCIA, numero, "Ana", texto);
	}

	private void processar(MensagemRecebida mensagem) {
		EvolutionWebhookEvento evento = evento("messages.upsert", mensagem.instancia());
		when(webhookService.numeroDoRemetente(evento)).thenReturn(Optional.of(mensagem.numero()));
		when(webhookService.extrairImagem(evento)).thenReturn(Optional.empty());
		when(webhookService.processar(evento)).thenReturn(Optional.of(mensagem));
		useCase.executar(evento);
	}

	private EvolutionWebhookEvento evento(String tipo, String instancia) {
		return new EvolutionWebhookEvento(tipo, instancia, JsonMapper.builder().build().createObjectNode());
	}

	private void verificarResposta(MensagemRecebida destinatario, String texto) {
		verify(evolutionClient, atLeastOnce()).enviarMensagem(eq(destinatario.instancia()),
				eq(new MensagemEnviadaDTO(destinatario.numero(), texto)));
	}
}
