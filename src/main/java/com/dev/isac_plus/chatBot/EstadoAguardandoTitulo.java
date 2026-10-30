package com.dev.isac_plus.chatBot;

import org.springframework.stereotype.Component;
import com.dev.isac_plus.dto.MensagemRecebida;
import com.dev.isac_plus.dto.MensagemEnviadaDTO;
import com.dev.isac_plus.restClient.EvolutionRestClient;



@Component 
public class EstadoAguardandoTitulo implements EstadoConversaI {
    private static final String PEDIDO_TITULO_NOVAMENTE = "Não consegui identificar um título nessa mensagem.\n\n"
            + "Por favor, envie o título que deseja usar no anúncio.";

    private final EvolutionRestClient evolutionClient;
    private final EstadoAguardandoTexto proximoEstado;
    
    public EstadoAguardandoTitulo(EvolutionRestClient evolutionClient, EstadoAguardandoTexto proximoEstado) {
        this.evolutionClient = evolutionClient;
        this.proximoEstado = proximoEstado;
    }
    
    public void processarMensagem(MensagemRecebida mensagem, ContextoConversa contexto) {
        if (mensagem.texto() == null || mensagem.texto().isBlank()) {
                responder(mensagem, PEDIDO_TITULO_NOVAMENTE);
                return;
        }

            contexto.avancarPara(proximoEstado);
            return;
    }

    private void responder(MensagemRecebida mensagem, String texto) {
        MensagemEnviadaDTO resposta =
                new MensagemEnviadaDTO(mensagem.numero(), texto);

        evolutionClient.enviarMensagem(mensagem.instancia(), resposta);
    }
            
}

    
    

