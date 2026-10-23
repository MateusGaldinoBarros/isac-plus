package com.dev.isac_plus.service;

import com.dev.isac_plus.dto.LoginDTO;
import com.dev.isac_plus.dto.RegisterDTO;
import com.dev.isac_plus.model.Usuario;
import com.dev.isac_plus.repository.UsuarioRepository;
import com.dev.isac_plus.util.HashUtil;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UsuarioService {

    @Autowired private UsuarioRepository repository;

    // cadastro
    public Usuario cadastrar(RegisterDTO data) throws Exception {
        // se já tiver email no sistema
        if (repository.findByEmail(data.email()).isPresent()) {
            throw new Exception("Email já Registrado.");
        }

        // registrando novo usuario
        Usuario novoUsuario = new Usuario();
        novoUsuario.setNome(data.nome());
        novoUsuario.setEmail(data.email());

        novoUsuario.setSenha(HashUtil.hashSenha(data.senha()));

        return repository.save(novoUsuario);
    }

    // login
    public Usuario autenticar(LoginDTO data) throws Exception {

        // ver se o querido existe
        Optional<Usuario> usuarioOptional =
            repository.findByEmail(data.email());

        if (usuarioOptional.isEmpty()) {
            throw new Exception("E-mail ou senha incorretos");
        }

        // catando o usuario e vendo se ele escreveu td certo
        Usuario usuario = usuarioOptional.get();

        String senhaCriptografadaRequest = HashUtil.hashSenha(data.senha());

        if (!usuario.getSenha().equals(senhaCriptografadaRequest)) {
            throw new Exception("E-mail ou senha incorretos");
        }

        return usuario;
    }
}
