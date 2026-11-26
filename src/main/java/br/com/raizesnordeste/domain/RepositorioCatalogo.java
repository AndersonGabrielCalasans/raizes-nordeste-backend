package br.com.raizesnordeste.domain;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RepositorioCatalogo {
    record Unidade(UUID id, String nome, String endereco) { }
    record Produto(UUID id, String nome, String descricao, BigDecimal preco, boolean disponivel) { }
    record Saldo(UUID produtoUnidadeId, UUID unidadeId, UUID produtoId, String nome, int saldo) { }
    record Pagina<T>(List<T> items, int page, int limit, long total) { }
    record Movimento(UUID id, int saldoAtual) { }

    Pagina<Unidade> unidades(int pagina, int limite);
    Optional<Unidade> unidade(UUID unidadeId);
    boolean unidadeExiste(UUID unidadeId);
    Pagina<Produto> cardapio(UUID unidadeId, int pagina, int limite);
    Optional<UUID> unidadeDoUsuario(UUID usuarioId);
    Pagina<Saldo> estoques(UUID unidadeId, int pagina, int limite);
    Optional<Integer> bloquearSaldo(UUID produtoUnidadeId);
    void atualizarSaldo(UUID produtoUnidadeId, int saldo);
    void inserirMovimento(UUID id, UUID produtoUnidadeId, String tipo, int quantidade);
}
