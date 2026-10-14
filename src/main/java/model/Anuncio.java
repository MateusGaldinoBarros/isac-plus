package model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class Anuncio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String titulo;

    private String descricao;

    private String contato;

    protected Anuncio() {
    }

    public Anuncio(String titulo, String descricao, String contato) {
        this.titulo = titulo;
        this.descricao = descricao;
        this.contato = contato;
    }
}

