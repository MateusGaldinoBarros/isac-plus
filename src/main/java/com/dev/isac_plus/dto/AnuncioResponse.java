package com.dev.isac_plus.dto;

import java.util.List;
import com.dev.isac_plus.model.Categoria;

public record AnuncioResponse(
        Long id, 
        String titulo, 
        String descricao, 
        String contato, 
        Categoria categoria, 
        List<ImagemResponse> imagens) {

    public AnuncioResponse(Long id, String titulo, String descricao, String contato) {
        this(id, titulo, descricao, contato, null, List.of());
    }
}