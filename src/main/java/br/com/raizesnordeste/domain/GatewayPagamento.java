package br.com.raizesnordeste.domain;

import java.math.BigDecimal;
import java.util.UUID;

public interface GatewayPagamento {
    enum Resultado { APROVADO, RECUSADO, FALHA }
    record Solicitacao(UUID tentativaId, UUID pedidoId, BigDecimal valor) { }
    Resultado solicitar(Solicitacao solicitacao) throws Exception;
}
