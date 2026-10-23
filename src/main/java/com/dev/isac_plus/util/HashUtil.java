package com.dev.isac_plus.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

public class HashUtil {
    public static String hashSenha(String senhaPura) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            // transforma a senha em bytes e hasha ela
            byte[] senhaBytes = senhaPura.getBytes(StandardCharsets.UTF_8);
            byte[] hashGerado = digest.digest(senhaBytes);

            return Base64.getEncoder().encodeToString(hashGerado);

        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Erro ao criptografar senha: algoritmo não encontrado", e);
        }
    }
}
