package com.dev.isac_plus.chatBot;
import org.springframework.stereotype.Component;

import com.dev.isac_plus.dto.MensagemEnviadaDTO;
import com.dev.isac_plus.dto.MensagemRecebida;
import com.dev.isac_plus.restClient.EvolutionRestClient;;

@Component 
public class EstadoInicial implements EstadoConversaI { 
    private final String mensagemBoasVindas = "Olá! 👋 Eu sou o Isac-plus, seu assistente para criação de anúncios";                                           
    private final EvolutionRestClient evolutionClient;
    private final EstadoAguardandoConfirmacao proximoEstado;;

    public EstadoInicial(EvolutionRestClient evolutionClient, EstadoAguardandoConfirmacao proximoEstado) {
        this.evolutionClient = evolutionClient;
        this.proximoEstado = proximoEstado;
    }

    @Override 
    public void processarMensagem(MensagemRecebida mensagemRecebida, ContextoConversa contexto) {
        responder(mensagemRecebida, mensagemBoasVindas);
        
        contexto.avancarPara(proximoEstado);
    }


    private void responder(MensagemRecebida mensagem, String texto) {
        MensagemEnviadaDTO resposta =
                new MensagemEnviadaDTO(mensagem.numero(), texto);

        evolutionClient.enviarMensagem(mensagem.instancia(), resposta);
    }
}
