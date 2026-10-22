package com.dev.isac_plus.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.dev.isac_plus.dto.EvolutionWebhookEvento;
import com.dev.isac_plus.useCase.CriarAnuncioUseCase;

@RestController
@RequestMapping("/webhook")
public class WebhookController {

	private final CriarAnuncioUseCase useCase;

	public WebhookController(CriarAnuncioUseCase useCase) {
		this.useCase = useCase;
	}

	// Responde 200 mesmo para eventos ignorados, senao a Evolution fica reenviando.
	@PostMapping("/evolution")
	public ResponseEntity<Void> receber(@RequestBody EvolutionWebhookEvento evento) {
		useCase.executar(evento);
		return ResponseEntity.ok().build();
	}
}
