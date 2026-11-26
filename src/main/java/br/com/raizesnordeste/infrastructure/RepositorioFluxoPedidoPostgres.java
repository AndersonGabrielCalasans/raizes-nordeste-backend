package br.com.raizesnordeste.infrastructure;

import br.com.raizesnordeste.domain.RepositorioFluxoPedido;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;
import javax.sql.DataSource;

@ApplicationScoped
public class RepositorioFluxoPedidoPostgres implements RepositorioFluxoPedido {
    @Inject DataSource banco;

    public Optional<Pedido> bloquearPedido(UUID id) {
        String consulta = "SELECT id,unidade_id,cliente_id,canal_pedido,status,situacao_pagamento,valor_total FROM pedidos WHERE id=? FOR UPDATE";
        try (var c = banco.getConnection(); var s = c.prepareStatement(consulta)) {
            s.setObject(1, id);
            try (var rs = s.executeQuery()) {
                return rs.next() ? Optional.of(new Pedido(rs.getObject(1, UUID.class), rs.getObject(2, UUID.class),
                        rs.getObject(3, UUID.class), rs.getString(4), rs.getString(5), rs.getString(6), rs.getBigDecimal(7))) : Optional.empty();
            }
        } catch (SQLException e) { throw falha(e); }
    }

    public Optional<Tentativa> pagamentoAprovado(Pedido pedido) {
        String consulta = "SELECT id,resultado FROM tentativas_pagamento WHERE pedido_id=? AND resultado='APROVADO' ORDER BY criado_em DESC LIMIT 1";
        try (var c = banco.getConnection(); var s = c.prepareStatement(consulta)) {
            s.setObject(1, pedido.id());
            try (var rs = s.executeQuery()) {
                return rs.next() ? Optional.of(new Tentativa(rs.getObject(1, UUID.class), pedido.id(), rs.getString(2), pedido.pagamento(), pedido.status())) : Optional.empty();
            }
        } catch (SQLException e) { throw falha(e); }
    }

    public void registrarTentativa(UUID id, Pedido pedido, String resultado) {
        String sql = "INSERT INTO tentativas_pagamento (id,pedido_id,valor_solicitado,resultado,falha) VALUES (?,?,?,?::situacao_pagamento,?)";
        try (var c = banco.getConnection(); var s = c.prepareStatement(sql)) {
            s.setObject(1, id); s.setObject(2, pedido.id()); s.setBigDecimal(3, pedido.total()); s.setString(4, resultado);
            s.setString(5, resultado.equals("FALHA") ? "Falha ou tempo limite do gateway mock" : null);
            s.executeUpdate();
        } catch (SQLException e) { throw falha(e); }
    }

    public void atualizarPedido(UUID id, String status, String pagamento) {
        try (var c = banco.getConnection(); var s = c.prepareStatement("UPDATE pedidos SET status=?::status_pedido,situacao_pagamento=?::situacao_pagamento WHERE id=?")) {
            s.setString(1, status); s.setString(2, pagamento); s.setObject(3, id); s.executeUpdate();
        } catch (SQLException e) { throw falha(e); }
    }

    public void restituirEstoque(UUID pedidoId) {
        String consulta = "SELECT produto_unidade_id,quantidade FROM itens_pedido WHERE pedido_id=? ORDER BY produto_unidade_id";
        String atualizar = "UPDATE produtos_unidades SET saldo_estoque=saldo_estoque+? WHERE id=?";
        String movimento = "INSERT INTO movimentacoes_estoque (id,produto_unidade_id,pedido_id,tipo,quantidade,origem) VALUES (?,?,?,'ENTRADA',?,'CANCELAMENTO')";
        try (var c = banco.getConnection(); var itens = c.prepareStatement(consulta);
                var estoque = c.prepareStatement(atualizar); var registro = c.prepareStatement(movimento)) {
            itens.setObject(1, pedidoId);
            try (var rs = itens.executeQuery()) {
                while (rs.next()) {
                    UUID produto = rs.getObject(1, UUID.class); int quantidade = rs.getInt(2);
                    estoque.setInt(1, quantidade); estoque.setObject(2, produto); estoque.executeUpdate();
                    registro.setObject(1, UUID.randomUUID()); registro.setObject(2, produto); registro.setObject(3, pedidoId);
                    registro.setInt(4, quantidade); registro.executeUpdate();
                }
            }
        } catch (SQLException e) { throw falha(e); }
    }

    public void registrarEstorno(UUID pedidoId) {
        try (var c = banco.getConnection(); var s = c.prepareStatement("UPDATE tentativas_pagamento SET estorno_simulado=true WHERE pedido_id=? AND resultado='APROVADO' AND estorno_simulado=false")) {
            s.setObject(1, pedidoId);
            if (s.executeUpdate() != 1) throw new IllegalStateException("Tentativa aprovada não encontrada para estorno");
        } catch (SQLException e) { throw falha(e); }
    }

    private IllegalStateException falha(SQLException e) { return new IllegalStateException("Falha ao persistir operação do pedido", e); }
}
