package com.dev.isac_plus.chatBot;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.dev.isac_plus.dto.ImagemRecebida;
import com.dev.isac_plus.dto.MensagemEnviadaDTO;
import com.dev.isac_plus.dto.MensagemRecebida;
import com.dev.isac_plus.model.Anuncio;
import com.dev.isac_plus.model.ImagemAnuncio;
import com.dev.isac_plus.repository.AnuncioRepository;
import com.dev.isac_plus.repository.ImagemAnuncioRepository;
import com.dev.isac_plus.restClient.EvolutionRestClient;
import com.dev.isac_plus.service.AnuncioService;
import com.dev.isac_plus.service.RegistroImagemService;

@Component
public class EstadoAguardandoTexto implements EstadoConversaI {

    private static final String ANUNCIO_CRIADO = "Anúncio criado com sucesso! 🎉\n\n"
            + "Agradeço por utilizar o Isac-plus. Se precisar de ajuda "
            + "com outro anúncio, é só me chamar.";
    private static final String PEDIDO_TEXTO_NOVAMENTE = "Não consegui identificar o texto do anúncio nessa mensagem.\n\n"
            + "Por favor, envie os detalhes que deseja publicar.";
    private final EvolutionRestClient evolutionClient;
    private final AnuncioService anuncioService;
    private final RegistroImagemService registroImagemService;
    private final AnuncioRepository anuncios;
    private final ImagemAnuncioRepository imagens;

    public EstadoAguardandoTexto(EvolutionRestClient evolutionClient, AnuncioService anuncioService,
            RegistroImagemService registroImagemService, AnuncioRepository anuncios,
            ImagemAnuncioRepository imagens) {
        this.evolutionClient = evolutionClient;
        this.anuncioService = anuncioService;
        this.registroImagemService = registroImagemService;
        this.anuncios = anuncios;
        this.imagens = imagens;
    }

    public Optional<ImagemAnuncio> processarImagem(ImagemRecebida recebida, ContextoConversa contexto) {
        Optional<ImagemAnuncio> imagem = registroImagemService.registrar(recebida, () -> escolherAnuncio(recebida));
        if (imagem.isPresent() && recebida.legenda() != null) {
            finalizarConversa(new MensagemRecebida(recebida.idWhatsapp(), recebida.instancia(),
                    recebida.numero(), null, recebida.legenda()), contexto);
        }
        return imagem;
    }

    private Anuncio escolherAnuncio(ImagemRecebida recebida) {
        if (recebida.idAlbum() != null) {
            Optional<Anuncio> doAlbum = imagens.findFirstByIdAlbumWhatsappOrderByIdAsc(recebida.idAlbum())
                    .map(ImagemAnuncio::getAnuncio);
            if (doAlbum.isPresent()) {
                Anuncio anuncio = doAlbum.get();
                if (recebida.legenda() != null && anuncio.isRascunho()) {
                    anuncio.publicarComDescricao(recebida.legenda(), recebida.idWhatsapp());
                }
                return anuncio;
            }
        }

        if (recebida.legenda() != null) {
            return anuncios.save(new Anuncio("", recebida.legenda(), recebida.numero(), recebida.idWhatsapp()));
        }

        Optional<Anuncio> recente = anuncioService.maisRecente(recebida.numero());
        if (recente.isPresent() && aceitaFotoSemLegenda(recente.get(), recebida)) {
            return recente.get();
        }

        return anuncios.save(Anuncio.rascunho(recebida.numero()));
    }

    private boolean aceitaFotoSemLegenda(Anuncio recente, ImagemRecebida recebida) {
        return recebida.idAlbum() == null || imagens.countByAnuncioId(recente.getId()) == 0;
    }

    public void processarMensagem(MensagemRecebida mensagem, ContextoConversa contexto) {
        if (mensagem.texto() == null
                || mensagem.texto().isBlank()) {
            responder(mensagem, PEDIDO_TEXTO_NOVAMENTE);
            return;
        }

        anuncioService.criarDeMensagem(
                mensagem,
                contexto.getTitulo(),
                mensagem.texto().trim());

        finalizarConversa(mensagem, contexto);
    }

    private void finalizarConversa(MensagemRecebida mensagem, ContextoConversa contexto) {
        contexto.encerrar();
        responder(mensagem, ANUNCIO_CRIADO);
    }

    private void responder(MensagemRecebida mensagem, String texto) {
        MensagemEnviadaDTO resposta =
                new MensagemEnviadaDTO(mensagem.numero(), texto);

        evolutionClient.enviarMensagem(mensagem.instancia(), resposta);
    }
}
