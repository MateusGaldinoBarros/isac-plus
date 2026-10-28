package com.dev.isac_plus.service;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.dev.isac_plus.exception.ImagemInvalidaException;

/**
 * Regras para aceitar uma imagem. Os metadados sao checados antes do download (para nao baixar a toa)
 * e o conteudo depois, porque o mimetype do webhook pode mentir.
 */
@Component
public class ValidadorImagem {

	public static final Set<String> TIPOS_ACEITOS = Set.of("image/jpeg", "image/png", "image/webp");
	public static final long TAMANHO_MAXIMO_BYTES = 10L * 1024 * 1024;
	public static final int MAXIMO_POR_ANUNCIO = 10;

	private static final byte[] ASSINATURA_JPEG = { (byte) 0xFF, (byte) 0xD8, (byte) 0xFF };
	private static final byte[] ASSINATURA_PNG = { (byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n' };
	private static final byte[] RIFF = { 'R', 'I', 'F', 'F' };
	private static final byte[] WEBP = { 'W', 'E', 'B', 'P' };

	/** Devolve o motivo da rejeicao, ou vazio se os metadados estao ok. Tamanho nulo passa (checado depois). */
	public Optional<String> validarMetadados(String mimetype, Long tamanhoBytes) {
		String tipo = normalizarTipo(mimetype);
		if (tipo == null) {
			return Optional.of("Tipo do arquivo nao informado");
		}
		if (!TIPOS_ACEITOS.contains(tipo)) {
			return Optional.of("Tipo " + tipo + " nao aceito; use JPEG, PNG ou WebP");
		}
		if (tamanhoBytes != null && tamanhoBytes > TAMANHO_MAXIMO_BYTES) {
			return Optional.of(tamanhoGrande(tamanhoBytes));
		}
		return Optional.empty();
	}

	/**
	 * Confere o arquivo baixado pelos primeiros bytes e devolve o tipo real dele.
	 *
	 * @throws ImagemInvalidaException se estiver vazio, grande demais ou nao for JPEG/PNG/WebP
	 */
	public String validarConteudo(byte[] conteudo) {
		if (conteudo == null || conteudo.length == 0) {
			throw new ImagemInvalidaException("Arquivo vazio");
		}
		if (conteudo.length > TAMANHO_MAXIMO_BYTES) {
			throw new ImagemInvalidaException(tamanhoGrande(conteudo.length));
		}
		if (comecaCom(conteudo, 0, ASSINATURA_JPEG)) {
			return "image/jpeg";
		}
		if (comecaCom(conteudo, 0, ASSINATURA_PNG)) {
			return "image/png";
		}
		// WebP: "RIFF" + 4 bytes de tamanho + "WEBP"
		if (comecaCom(conteudo, 0, RIFF) && comecaCom(conteudo, 8, WEBP)) {
			return "image/webp";
		}
		throw new ImagemInvalidaException("O conteudo nao e uma imagem JPEG, PNG ou WebP");
	}

	public Optional<String> validarQuantidade(long imagensNoAnuncio) {
		if (imagensNoAnuncio >= MAXIMO_POR_ANUNCIO) {
			return Optional.of("Anuncio ja tem o maximo de " + MAXIMO_POR_ANUNCIO + " imagens");
		}
		return Optional.empty();
	}

	/** "Image/JPEG; charset=x" vira "image/jpeg". Nulo ou vazio vira nulo. */
	public static String normalizarTipo(String mimetype) {
		if (mimetype == null) {
			return null;
		}
		int pontoEVirgula = mimetype.indexOf(';');
		String tipo = (pontoEVirgula >= 0 ? mimetype.substring(0, pontoEVirgula) : mimetype)
				.trim().toLowerCase(Locale.ROOT);
		return tipo.isEmpty() ? null : tipo;
	}

	private static boolean comecaCom(byte[] conteudo, int posicao, byte[] assinatura) {
		return conteudo.length >= posicao + assinatura.length
				&& Arrays.equals(conteudo, posicao, posicao + assinatura.length, assinatura, 0, assinatura.length);
	}

	private static String tamanhoGrande(long bytes) {
		return String.format(Locale.ROOT, "Imagem de %.1f MB passa do limite de 10 MB", bytes / (1024.0 * 1024));
	}
}
