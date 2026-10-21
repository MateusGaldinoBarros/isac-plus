package com.dev.isac_plus.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor 
@AllArgsConstructor 
@Table(name = "usuarios")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Auto-incremento
    private Long id;

    private String nome;

    @Column(unique = true, nullable = false) // Garante que não teremos emails duplicados
    private String email;

    @Column(nullable = false)
    private String senha;
}
