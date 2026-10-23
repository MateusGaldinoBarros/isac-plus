package com.dev.isac_plus.service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import org.springframework.stereotype.Service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import com.dev.isac_plus.model.Usuario;

@Service 
public class TokenService {
    private final String secret = "segredosecreto_que_ninguem_sabe";

    public String gerarToken(Usuario usuario) {
        try {

            Algorithm algorithm = Algorithm.HMAC256(secret);

            return JWT.create().withIssuer("isac-plus-api").withSubject(usuario.getEmail()).withExpiresAt(gerarDataExpiracao()).sign(algorithm);
                    

        } catch (JWTCreationException e) {
            throw new RuntimeException("Erro ao gerar token JWT", e);
        }
    }

    private Instant gerarDataExpiracao() {
        return LocalDateTime.now().plusHours(2).toInstant(ZoneOffset.of("-03:00"));
    }
}
