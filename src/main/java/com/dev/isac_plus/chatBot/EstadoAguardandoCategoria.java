package com.dev.isac_plus.chatBot;

import java.util.Arrays;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.dev.isac_plus.dto.MensagemEnviadaDTO;
import com.dev.isac_plus.dto.MensagemRecebida;
import com.dev.isac_plus.model.Categoria;
import com.dev.isac_plus.restClient.EvolutionRestClient;

@Component
public class EstadoAguardandoCategoria implements EstadoConversaI {

    private static final String INSTRUCAO = "Escolha a categoria do anúncio respondendo com um número:";
    private static final String PEDIDO_TITULO = "Por favor, informe o título que gostaria de usar no anúncio.";

    private final EvolutionRestClient evolutionClient;
    private final EstadoAguardandoTitulo proximoEstado;

    public EstadoAguardandoCategoria(EvolutionRestClient evolutionClient, EstadoAguardandoTitulo proximoEstado) {
        this.evolutionClient = evolutionClient;
        this.proximoEstado = proximoEstado;
    }

    static String mensagemOpcoes() {
        String opcoes = Arrays.stream(Categoria.values())
                .map(categoria -> (categoria.ordinal() + 1) + ". " + rotulo(categoria))
                .collect(Collectors.joining("\n"));
        return INSTRUCAO + "\n" + opcoes;
    }

    @Override
    public void processarMensagem(MensagemRecebida mensagem, ContextoConversa contexto) {
        Categoria categoria = categoriaDoNumero(mensagem.texto());
        if (categoria == null) {
            responder(mensagem, mensagemOpcoes());
            return;
        }

        contexto.setCategoria(categoria);
        responder(mensagem, PEDIDO_TITULO);
        contexto.avancarPara(proximoEstado);
    }

    private Categoria categoriaDoNumero(String texto) {
        if (texto == null) {
            return null;
        }
        int numero;
        try {
            numero = Integer.parseInt(texto.trim());
        } catch (NumberFormatException e) {
            return null;
        }
        Categoria[] categorias = Categoria.values();
        if (numero < 1 || numero > categorias.length) {
            return null;
        }
        return categorias[numero - 1];
    }

    private static String rotulo(Categoria categoria) {
        return switch (categoria) {
            case ELETRONICOS -> "Eletrônicos";
            case IMOVEIS -> "Imóveis";
            case VEICULOS -> "Veículos";
            case SERVICOS -> "Serviços";
            case OUTROS -> "Outros";
        };
    }

    private void responder(MensagemRecebida mensagem, String texto) {
        evolutionClient.enviarMensagem(mensagem.instancia(),
                new MensagemEnviadaDTO(mensagem.numero(), texto));
    }
}
