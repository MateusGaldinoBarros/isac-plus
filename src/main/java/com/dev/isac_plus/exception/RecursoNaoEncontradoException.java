package com.dev.isac_plus.exception;

public class RecursoNaoEncontradoException extends RuntimeException {

	public RecursoNaoEncontradoException(String recurso, Object id) {
		super(recurso + " com id " + id + " nao encontrado");
	}
}
