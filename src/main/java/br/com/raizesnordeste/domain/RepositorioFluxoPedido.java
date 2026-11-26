package br.com.raizesnordeste.domain;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

public interface RepositorioFluxoPedido {
    record Pedido(UUID id, UUID unidadeId, UUID clienteId, String canal, String status, String pagamento, BigDecimal total) { }
    record Tentativa(UUID tentativaId, UUID pedidoId, String resultado, String situacaoPagamento, String statusPedido) { }

    Optional<Pedido> bloquearPedido(UUID id);
    Optional<Tentativa> pagamentoAprovado(Pedido pedido);
    void registrarTentativa(UUID tentativaId, Pedido pedido, String resultado);
    void atualizarPedido(UUID id, String status, String pagamento);
    void restituirEstoque(UUID pedidoId);
    void registrarEstorno(UUID pedidoId);
}
