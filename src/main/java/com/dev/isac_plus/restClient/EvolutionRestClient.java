package com.dev.isac_plus.restClient;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.dev.isac_plus.dto.MensagemEnviadaDTO;

import org.springframework.beans.factory.annotation.Qualifier;

@Component 
public class EvolutionRestClient {
    private final RestClient restClient;

    public EvolutionRestClient(
            @Qualifier("evolutionHttpRestClient") RestClient evolutionRestClient) {
        this.restClient = evolutionRestClient;
    }

    public void enviarMensagem(
            String instancia,
            MensagemEnviadaDTO mensagem) {

        restClient.post()
                .uri("/message/sendText/{instance}", instancia)
                .body(mensagem)
                .retrieve()
                .toBodilessEntity();
    }
}
