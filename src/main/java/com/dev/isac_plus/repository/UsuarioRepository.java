package com.dev.isac_plus.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.dev.isac_plus.model.Usuario;

public interface UsuarioRepository extends JpaRepository<Usuario, Long>{
    Optional<Usuario> findByEmail(String email);
    
}