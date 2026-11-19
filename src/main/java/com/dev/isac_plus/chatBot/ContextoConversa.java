package com.dev.isac_plus.chatBot;
import com.dev.isac_plus.dto.MensagemRecebida;
import com.dev.isac_plus.model.Categoria;

public class ContextoConversa {
    private EstadoConversaI estadoAtual;
    private String titulo;
    private Categoria categoria;
    public boolean encerrada;
    

    public ContextoConversa(EstadoConversaI estadoInicial, String titulo) {
        this.estadoAtual = estadoInicial;
        this.titulo = titulo;
    }

    public void avancarPara(EstadoConversaI novoEstado) {
        this.estadoAtual = novoEstado;
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

    public Categoria getCategoria() {
        return categoria;
    }

    public void setCategoria(Categoria categoria) {
        this.categoria = categoria;
    }

    public boolean isEncerrada() {
        return encerrada;
    }

    public void encerrar() {
        this.encerrada = true;
    }

    public void processarMensagem(MensagemRecebida mensagemRecebida) {
        if (estadoAtual != null) {
            estadoAtual.processarMensagem(mensagemRecebida, this);
        }
    }
}
