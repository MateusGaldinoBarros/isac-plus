package com.dev.isac_plus.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AnuncioRequest(
		@NotBlank @Size(max = 120) String titulo,
		@Size(max = 2000) String descricao,
		@NotBlank @Size(max = 30) String contato) {
}
