package br.com.raizesnordeste.domain;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RepositorioPedidos {
    record Ator(UUID id, String perfil, UUID unidadeId) {
    }

    record Produto(UUID id, UUID unidadeId, BigDecimal preco, boolean disponivel, int saldo) {
    }

    record Item(UUID produtoUnidadeId, int quantidade, BigDecimal precoUnitario, BigDecimal subtotal) {
    }

    record Pedido(UUID id, UUID unidadeId, UUID clienteId, UUID criadorId, String canalPedido,
            String status, String situacaoPagamento, BigDecimal valorTotal, List<Item> itens) {
    }

    record Pagina(List<Pedido> items, int page, int limit, long total) {
    }

    Optional<Ator> ator(UUID id);

    boolean unidadeExiste(UUID id);

    Optional<Produto> bloquearProduto(UUID produtoUnidadeId);

    void gravar(Pedido pedido);

    void baixar(UUID produtoUnidadeId, UUID pedidoId, int quantidade);

    Optional<Pedido> buscar(UUID id);

    Pagina listar(Ator ator, String canal, String status, int pagina, int limite);
}
