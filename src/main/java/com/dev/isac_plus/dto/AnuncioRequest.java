package com.dev.isac_plus.dto;

import com.dev.isac_plus.model.Categoria;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AnuncioRequest(
        @NotBlank @Size(max = 120) String titulo,
        @Size(max = 2000) String descricao,
        @NotBlank @Size(max = 30) String contato,
        @NotNull Categoria categoria) {

    // Construtor auxiliar para manter compatibilidade com os testes antigos
    public AnuncioRequest(String titulo, String descricao, String contato) {
        this(titulo, descricao, contato, Categoria.OUTROS);
    }
}