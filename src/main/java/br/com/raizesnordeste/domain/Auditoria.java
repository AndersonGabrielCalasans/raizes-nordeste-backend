package br.com.raizesnordeste.domain;

import java.util.UUID;

public interface Auditoria {
    record Evento(UUID responsavelId, String operacao, String recurso, UUID recursoId, String resultado) { }

    void registrar(Evento evento);
}
