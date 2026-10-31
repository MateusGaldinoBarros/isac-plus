package com.dev.isac_plus.chatBot;
import com.dev.isac_plus.dto.MensagemRecebida;

import org.springframework.stereotype.Component;

import com.dev.isac_plus.dto.MensagemEnviadaDTO;
import com.dev.isac_plus.restClient.EvolutionRestClient;
import com.dev.isac_plus.service.AnuncioService;

@Component 
public class EstadoAguardandoTexto implements EstadoConversaI{
    
    private static final String ANUNCIO_CRIADO = "Anúncio criado com sucesso! 🎉\n\n"
            + "Agradeço por utilizar o Isac-plus. Se precisar de ajuda "
            + "com outro anúncio, é só me chamar.";
    private static final String PEDIDO_TEXTO_NOVAMENTE = "Não consegui identificar o texto do anúncio nessa mensagem.\n\n"
            + "Por favor, envie os detalhes que deseja publicar.";
    private final EvolutionRestClient evolutionClient;
    private final AnuncioService anuncioService;


    public EstadoAguardandoTexto(EvolutionRestClient evolutionClient, AnuncioService anuncioService) {
        this.evolutionClient = evolutionClient;
        this.anuncioService = anuncioService;
    }
    
    public void processarMensagem(MensagemRecebida mensagem, ContextoConversa contexto) {
        if (mensagem.texto() == null
                || mensagem.texto().isBlank()) {
            responder(mensagem, PEDIDO_TEXTO_NOVAMENTE);
            return;
        }

        anuncioService.criarDeMensagem(
                mensagem,
                contexto.getTitulo(),
                mensagem.texto().trim());


            contexto.encerrar();
            responder(mensagem, ANUNCIO_CRIADO);
    }

    private void responder(MensagemRecebida mensagem, String texto) {
        MensagemEnviadaDTO resposta =
                new MensagemEnviadaDTO(mensagem.numero(), texto);

        evolutionClient.enviarMensagem(mensagem.instancia(), resposta);
    }
}
