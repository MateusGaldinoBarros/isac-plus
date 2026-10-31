package com.dev.isac_plus.chatBot;

import java.text.Normalizer;
import java.util.Locale;

import org.springframework.stereotype.Component;

import com.dev.isac_plus.dto.MensagemEnviadaDTO;
import com.dev.isac_plus.dto.MensagemRecebida;
import com.dev.isac_plus.restClient.EvolutionRestClient;


@Component 
public class EstadoAguardandoConfirmacao implements EstadoConversaI {
    private static final String ANUNCIO_CANCELADO = "Tudo bem, não criarei o anúncio. 😊\n\n"
            + "Quando quiser começar, basta enviar uma nova mensagem. "
            + "Estarei à disposição!";
    private static final String PEDIDO_CONFIRMACAO = "Gostaria de criar um anúncio? Responda sim ou não para continuarmos.";
    private static final String PEDIDO_TITULO = "Que bom poder ajudar! 😊\n\n"
            + "Por favor, informe o título que gostaria de usar no anúncio.";
    private final EvolutionRestClient evolutionClient;
    private final EstadoAguardandoTitulo proximoEstado;

    public EstadoAguardandoConfirmacao(EvolutionRestClient evolutionClient, EstadoAguardandoTitulo proximoEstado) {
        this.evolutionClient = evolutionClient;
        this.proximoEstado = proximoEstado;
    }
    
    
    
    public void processarMensagem(MensagemRecebida mensagem, ContextoConversa contexto) {
        
        String resposta = normalizar(mensagem.texto());

            if ("sim".equals(resposta)) {
                responder(mensagem, PEDIDO_TITULO);
                contexto.avancarPara(proximoEstado);

                

            } else if ("nao".equals(resposta)) {
                contexto.encerrar();
                responder(mensagem, ANUNCIO_CANCELADO);

            } else {
                responder(mensagem, PEDIDO_CONFIRMACAO);
            }

            return;
    }


    private String normalizar(String texto) {
        if (texto == null) {
            return "";
        }

        return Normalizer
                .normalize(
                        texto.trim().toLowerCase(Locale.ROOT),
                        Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
    }

    private void responder(MensagemRecebida mensagem, String texto) {
        MensagemEnviadaDTO resposta =
                new MensagemEnviadaDTO(mensagem.numero(), texto);

        evolutionClient.enviarMensagem(mensagem.instancia(), resposta);
    }
    
   
    
}
