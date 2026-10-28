package com.dev.isac_plus.service;

import java.util.concurrent.locks.ReentrantLock;

import org.springframework.stereotype.Component;

/**
 * Faz mensagens do mesmo numero serem processadas uma de cada vez.
 * A Evolution manda as fotos de um album em webhooks paralelos; sem isso, duas fotos poderiam
 * nao enxergar o rascunho uma da outra e criar dois anuncios.
 *
 * Usa um numero fixo de travas (o numero cai sempre na mesma), entao a memoria nao cresce com
 * a quantidade de contatos. Numeros diferentes podem dividir uma trava; so esperam um pouco.
 * Vale para uma instancia do backend; com varias, trocar por uma trava no banco (pg_advisory_xact_lock).
 */
@Component
public class TravaPorNumero {

	private static final int QUANTIDADE = 64;

	private final ReentrantLock[] travas = new ReentrantLock[QUANTIDADE];

	public TravaPorNumero() {
		for (int i = 0; i < QUANTIDADE; i++) {
			travas[i] = new ReentrantLock();
		}
	}

	public void executar(String numero, Runnable acao) {
		ReentrantLock trava = travas[Math.floorMod(numero == null ? 0 : numero.hashCode(), QUANTIDADE)];
		trava.lock();
		try {
			acao.run();
		} finally {
			trava.unlock();
		}
	}
}
