package br.com.raizesnordeste.domain;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public interface RepositorioUsuarios {

    record Registro(UUID id, String nome, String cpf, LocalDate nascimento, String email, String senhaHash,
            String perfil, UUID unidadeId) {
    }

    boolean unidadeExiste(UUID id);

    void inserir(Registro usuario);

    Optional<Registro> buscarPorEmail(String email);

    Optional<Registro> alterarUnidade(UUID id, UUID unidadeId);
}
