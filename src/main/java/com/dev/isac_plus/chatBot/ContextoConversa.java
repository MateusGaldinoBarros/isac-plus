package com.dev.isac_plus.chatBot;

public class ContextoConversa {
    private EstadoConversaI estadoAtual;
    
    public ContextoConversa(EstadoConversaI estadoInicial) {
        this.estadoAtual = estadoInicial;
    }

    public void avancarPara(EstadoConversaI novoEstado) {
        this.estadoAtual = novoEstado;
    }
    
    public EstadoConversaI getEstadoAtual() {
        return estadoAtual;
    }
}