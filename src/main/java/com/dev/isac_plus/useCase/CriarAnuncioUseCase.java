
package com.dev.isac_plus.useCase;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionOperations;

import com.dev.isac_plus.chatBot.ContextoConversa;
import com.dev.isac_plus.dto.EvolutionWebhookEvento;
import com.dev.isac_plus.dto.ImagemRecebida;
import com.dev.isac_plus.dto.MensagemRecebida;
import com.dev.isac_plus.service.RegistroImagemService;
import com.dev.isac_plus.service.TravaPorNumero;
import com.dev.isac_plus.service.WebhookService;
import com.dev.isac_plus.chatBot.EstadoInicial;

@Service
public class CriarAnuncioUseCase {

    private final WebhookService webhookService;
    private final RegistroImagemService registroImagemService;
    private final TravaPorNumero trava;
    private final TransactionOperations transacao;
    private final EstadoInicial estadoInicial;
    private record Contato(String instancia, String numero) {
    }

    private final ConcurrentHashMap<Contato, ContextoConversa> conversas =
            new ConcurrentHashMap<>();

    

    public CriarAnuncioUseCase(
            WebhookService webhookService,
            RegistroImagemService registroImagemService,
            TravaPorNumero trava,
            TransactionOperations transacao, EstadoInicial estadoInicial) {

        this.webhookService = webhookService;
        this.registroImagemService = registroImagemService;
        this.trava = trava;
        this.transacao = transacao;
        this.estadoInicial = estadoInicial;
    }

    public void executar(EvolutionWebhookEvento evento) {
        Optional<String> remetente =
                webhookService.numeroDoRemetente(evento);

        if (remetente.isEmpty()) {
            return;
        }

        String numero = remetente.get();

        trava.executar(numero, () ->
                transacao.executeWithoutResult(status ->
                        processarEvento(evento)));
    }

    private void processarEvento(EvolutionWebhookEvento evento) {
        Optional<ImagemRecebida> imagem =
                webhookService.extrairImagem(evento);

        if (imagem.isPresent()) {
            registroImagemService.registrar(imagem.get());
            return;
        }

        webhookService.processar(evento)
                .ifPresent(this::processarMensagem);
    }

    private void processarMensagem(MensagemRecebida mensagem) {
        Contato contato =
                new Contato(mensagem.instancia(), mensagem.numero());

        ContextoConversa contexto = conversas.computeIfAbsent(
                contato,
                chave -> new ContextoConversa(estadoInicial, "")
        );

        contexto.processarMensagem(mensagem);

        if (contexto.isEncerrada()) {
            conversas.remove(contato, contexto);
        }
    }
}