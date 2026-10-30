package com.dev.isac_plus.chatBot;

public class ContextoConversa {
    private EstadoConversaI estadoAtual;
    private String titulo;

    public ContextoConversa(EstadoConversaI estadoInicial, String titulo) {
        this.estadoAtual = estadoInicial;
        this.titulo = titulo;
    }

    public void avancarPara(EstadoConversaI novoEstado) {
        this.estadoAtual = novoEstado;
    }

    public void removerContexto() {
        this.estadoAtual = null;
    }
    
    public EstadoConversaI getEstadoAtual() {
        return estadoAtual;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }
}