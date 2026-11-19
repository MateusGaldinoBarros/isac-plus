package com.dev.isac_plus.chatBot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.dev.isac_plus.dto.MensagemEnviadaDTO;
import com.dev.isac_plus.dto.MensagemRecebida;
import com.dev.isac_plus.model.Categoria;
import com.dev.isac_plus.restClient.EvolutionRestClient;

class EstadoAguardandoCategoriaTest {

    private final EvolutionRestClient evolutionClient = mock(EvolutionRestClient.class);
    private final EstadoAguardandoTitulo proximoEstado = mock(EstadoAguardandoTitulo.class);
    private EstadoAguardandoCategoria estado;
    private ContextoConversa contexto;

    @BeforeEach
    void setUp() {
        estado = new EstadoAguardandoCategoria(evolutionClient, proximoEstado);
        contexto = new ContextoConversa(estado, "");
    }

    @Test
    void categoriaInvalidaRepeteOpcoesSemAvancar() {
        estado.processarMensagem(mensagem("6"), contexto);

        assertThat(contexto.getEstadoAtual()).isSameAs(estado);
        assertThat(contexto.getCategoria()).isNull();
        ArgumentCaptor<MensagemEnviadaDTO> resposta = ArgumentCaptor.forClass(MensagemEnviadaDTO.class);
        verify(evolutionClient).enviarMensagem(eq("ong"), resposta.capture());
        assertThat(resposta.getValue().text()).contains("1. Eletrônicos", "5. Outros");
    }

    @Test
    void entradaNaoNumericaRepeteOpcoesSemAvancar() {
        estado.processarMensagem(mensagem("eletrônicos"), contexto);

        assertThat(contexto.getEstadoAtual()).isSameAs(estado);
        assertThat(contexto.getCategoria()).isNull();
        verify(evolutionClient).enviarMensagem(eq("ong"), any(MensagemEnviadaDTO.class));
    }

    @Test
    void numeroValidoDefineCategoriaEPedeTitulo() {
        estado.processarMensagem(mensagem(" 3 "), contexto);

        assertThat(contexto.getCategoria()).isEqualTo(Categoria.VEICULOS);
        assertThat(contexto.getEstadoAtual()).isSameAs(proximoEstado);
        verify(proximoEstado, never()).processarMensagem(any(), any());
        verify(evolutionClient).enviarMensagem(eq("ong"), any(MensagemEnviadaDTO.class));
    }

    private MensagemRecebida mensagem(String texto) {
        return new MensagemRecebida("id", "ong", "5511999", "Ana", texto);
    }
}
