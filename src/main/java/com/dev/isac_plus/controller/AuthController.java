package com.dev.isac_plus.controller;

import com.dev.isac_plus.dto.LoginDTO;
import com.dev.isac_plus.dto.RegisterDTO;
import com.dev.isac_plus.model.Usuario;
import com.dev.isac_plus.service.UsuarioService;
import org.apache.catalina.connector.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {
    @Autowired private UsuarioService usuarioService;

    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody RegisterDTO data) {
        try {
            usuarioService.cadastrar(data);

            return ResponseEntity.status(HttpStatus.CREATED)
                .body("Usuario cadastrado com sucesso");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(e.getMessage());
        }
    }

    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody LoginDTO data) {
        try {
            Usuario usuarioLogado = usuarioService.autenticar(data);

            return ResponseEntity.ok("Login realizado com sucesso!");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        }
    }
}
