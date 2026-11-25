package br.com.raizesnordeste.domain;

import java.time.Instant;
import java.util.UUID;

public interface Credenciais {
    String gerarHash(String senha);
    boolean conferirSenha(String senha, String hash);
    String gerarToken(UUID id, String perfil, Instant expiracao);
}
