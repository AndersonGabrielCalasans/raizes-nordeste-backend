package br.com.raizesnordeste.infrastructure;

import br.com.raizesnordeste.domain.RepositorioPedidos;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.sql.DataSource;

@ApplicationScoped
public class RepositorioPedidosPostgres implements RepositorioPedidos {
    @Inject
    DataSource banco;

    public Optional<Ator> ator(UUID id) {
        try (var c = banco.getConnection();
                var s = c.prepareStatement("SELECT id,perfil,unidade_id FROM usuarios WHERE id=?")) {
            s.setObject(1, id);
            try (var rs = s.executeQuery()) {
                return rs.next()
                        ? Optional
                                .of(new Ator(rs.getObject(1, UUID.class), rs.getString(2), rs.getObject(3, UUID.class)))
                        : Optional.empty();
            }
        } catch (SQLException e) {
            throw falha(e);
        }
    }

    public boolean unidadeExiste(UUID id) {
        try (var c = banco.getConnection(); var s = c.prepareStatement("SELECT 1 FROM unidades WHERE id=?")) {
            s.setObject(1, id);
            try (var rs = s.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw falha(e);
        }
    }

    public Optional<Produto> bloquearProduto(UUID id) {
        try (var c = banco.getConnection();
                var s = c.prepareStatement(
                        "SELECT id,unidade_id,preco,disponivel,saldo_estoque FROM produtos_unidades WHERE id=? FOR UPDATE")) {
            s.setObject(1, id);
            try (var rs = s.executeQuery()) {
                return rs.next()
                        ? Optional.of(new Produto(rs.getObject(1, UUID.class), rs.getObject(2, UUID.class),
                                rs.getBigDecimal(3), rs.getBoolean(4), rs.getInt(5)))
                        : Optional.empty();
            }
        } catch (SQLException e) {
            throw falha(e);
        }
    }

    public void gravar(Pedido p) {
        String cabecalho = "INSERT INTO pedidos (id,unidade_id,cliente_id,criador_id,canal_pedido,status,situacao_pagamento,valor_total) VALUES (?,?,?,?,?::canal_pedido,?::status_pedido,?::situacao_pagamento,?)";
        String item = "INSERT INTO itens_pedido (id,pedido_id,produto_unidade_id,quantidade,preco_unitario,subtotal) VALUES (?,?,?,?,?,?)";
        try (var c = banco.getConnection(); var s = c.prepareStatement(cabecalho); var i = c.prepareStatement(item)) {
            s.setObject(1, p.id());
            s.setObject(2, p.unidadeId());
            s.setObject(3, p.clienteId());
            s.setObject(4, p.criadorId());
            s.setString(5, p.canalPedido());
            s.setString(6, p.status());
            s.setString(7, p.situacaoPagamento());
            s.setBigDecimal(8, p.valorTotal());
            s.executeUpdate();
            for (var linha : p.itens()) {
                i.setObject(1, UUID.randomUUID());
                i.setObject(2, p.id());
                i.setObject(3, linha.produtoUnidadeId());
                i.setInt(4, linha.quantidade());
                i.setBigDecimal(5, linha.precoUnitario());
                i.setBigDecimal(6, linha.subtotal());
                i.executeUpdate();
            }
        } catch (SQLException e) {
            throw falha(e);
        }
    }

    public void baixar(UUID produto, UUID pedido, int quantidade) {
        try (var c = banco.getConnection();
                var s = c.prepareStatement("UPDATE produtos_unidades SET saldo_estoque=saldo_estoque-? WHERE id=?");
                var m = c.prepareStatement(
                        "INSERT INTO movimentacoes_estoque (id,produto_unidade_id,pedido_id,tipo,quantidade,origem) VALUES (?,?,?,'SAIDA',?,'PEDIDO')")) {
            s.setInt(1, quantidade);
            s.setObject(2, produto);
            s.executeUpdate();
            m.setObject(1, UUID.randomUUID());
            m.setObject(2, produto);
            m.setObject(3, pedido);
            m.setInt(4, quantidade);
            m.executeUpdate();
        } catch (SQLException e) {
            throw falha(e);
        }
    }

    private static final String COLUNAS = "id,unidade_id,cliente_id,criador_id,canal_pedido,status,situacao_pagamento,valor_total";

    private Pedido ler(Connection c, ResultSet rs) throws SQLException {
        UUID id = rs.getObject("id", UUID.class);
        var itens = new ArrayList<Item>();
        try (var s = c.prepareStatement(
                "SELECT produto_unidade_id,quantidade,preco_unitario,subtotal FROM itens_pedido WHERE pedido_id=? ORDER BY produto_unidade_id")) {
            s.setObject(1, id);
            try (var i = s.executeQuery()) {
                while (i.next())
                    itens.add(
                            new Item(i.getObject(1, UUID.class), i.getInt(2), i.getBigDecimal(3), i.getBigDecimal(4)));
            }
        }
        return new Pedido(id, rs.getObject("unidade_id", UUID.class), rs.getObject("cliente_id", UUID.class),
                rs.getObject("criador_id", UUID.class), rs.getString("canal_pedido"), rs.getString("status"),
                rs.getString("situacao_pagamento"), rs.getBigDecimal("valor_total"), List.copyOf(itens));
    }

    public Optional<Pedido> buscar(UUID id) {
        try (var c = banco.getConnection();
                var s = c.prepareStatement("SELECT " + COLUNAS + " FROM pedidos WHERE id=?")) {
            s.setObject(1, id);
            try (var rs = s.executeQuery()) {
                return rs.next() ? Optional.of(ler(c, rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw falha(e);
        }
    }

    public Pagina listar(Ator ator, String canal, String status, int pagina, int limite) {
        StringBuilder where = new StringBuilder(" WHERE 1=1");
        var parametros = new ArrayList<Object>();
        switch (ator.perfil()) {
            case "CLIENTE" -> {
                where.append(" AND cliente_id=?");
                parametros.add(ator.id());
            }
            case "ATENDENTE", "COZINHA" -> {
                where.append(" AND unidade_id=?");
                parametros.add(ator.unidadeId());
            }
            case "ADMINISTRADOR" -> {
            }
            default -> where.append(" AND false");
        }
        if (ator.perfil().equals("COZINHA"))
            where.append(" AND (canal_pedido='BALCAO' OR situacao_pagamento='APROVADO')");
        if (canal != null) {
            where.append(" AND canal_pedido=?::canal_pedido");
            parametros.add(canal);
        }
        if (status != null) {
            where.append(" AND status=?::status_pedido");
            parametros.add(status);
        }
        try (var c = banco.getConnection();
                var count = c.prepareStatement("SELECT count(*) FROM pedidos" + where);
                var sql = c.prepareStatement("SELECT " + COLUNAS + " FROM pedidos" + where
                        + " ORDER BY criado_em DESC,id LIMIT ? OFFSET ?")) {
            bind(count, parametros);
            bind(sql, parametros);
            sql.setInt(parametros.size() + 1, limite);
            sql.setInt(parametros.size() + 2, (pagina - 1) * limite);
            long total;
            try (var rs = count.executeQuery()) {
                rs.next();
                total = rs.getLong(1);
            }
            var itens = new ArrayList<Pedido>();
            try (var rs = sql.executeQuery()) {
                while (rs.next())
                    itens.add(ler(c, rs));
            }
            return new Pagina(List.copyOf(itens), pagina, limite, total);
        } catch (SQLException e) {
            throw falha(e);
        }
    }

    private void bind(PreparedStatement s, List<Object> valores) throws SQLException {
        for (int i = 0; i < valores.size(); i++)
            s.setObject(i + 1, valores.get(i));
    }

    private IllegalStateException falha(SQLException e) {
        return new IllegalStateException("Falha na persistência de pedidos", e);
    }
}
