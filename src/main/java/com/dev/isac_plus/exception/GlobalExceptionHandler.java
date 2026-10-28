package com.dev.isac_plus.exception;

import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	@ExceptionHandler(RecursoNaoEncontradoException.class)
	public ResponseEntity<ErroResponse> naoEncontrado(RecursoNaoEncontradoException ex) {
		return resposta(HttpStatus.NOT_FOUND, new ErroResponse(404, ex.getMessage()));
	}

	@ExceptionHandler(StorageException.class)
	public ResponseEntity<ErroResponse> storageIndisponivel(StorageException ex) {
		log.error("Storage indisponivel", ex);
		return resposta(HttpStatus.SERVICE_UNAVAILABLE, new ErroResponse(503, "Imagem indisponivel no momento"));
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ErroResponse> validacao(MethodArgumentNotValidException ex) {
		Map<String, String> campos = new LinkedHashMap<>();
		for (FieldError erro : ex.getBindingResult().getFieldErrors()) {
			campos.putIfAbsent(erro.getField(), erro.getDefaultMessage());
		}
		return resposta(HttpStatus.BAD_REQUEST, new ErroResponse(400, "Dados invalidos", campos));
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<ErroResponse> corpoInvalido(HttpMessageNotReadableException ex) {
		return resposta(HttpStatus.BAD_REQUEST, new ErroResponse(400, "Corpo da requisicao invalido"));
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErroResponse> erroInesperado(Exception ex) {
		log.error("Erro inesperado", ex);
		return resposta(HttpStatus.INTERNAL_SERVER_ERROR, new ErroResponse(500, "Erro interno no servidor"));
	}

	private ResponseEntity<ErroResponse> resposta(HttpStatus status, ErroResponse corpo) {
		return ResponseEntity.status(status).body(corpo);
	}
}
