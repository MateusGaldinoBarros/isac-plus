package com.dev.isac_plus.chatBot;
import com.dev.isac_plus.dto.MensagemRecebida;

public interface EstadoConversaI {
    void processarMensagem(MensagemRecebida mensagemRecebida, ContextoConversa contexto);
}
