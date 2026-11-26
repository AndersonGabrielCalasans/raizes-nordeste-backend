package br.com.raizesnordeste.infrastructure;

import br.com.raizesnordeste.domain.RepositorioCatalogo;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;
import javax.sql.DataSource;

@ApplicationScoped
public class RepositorioCatalogoPostgres implements RepositorioCatalogo {
    @Inject DataSource banco;

    @FunctionalInterface interface Leitor<T> { T ler(ResultSet rs) throws SQLException; }

    private <T> Pagina<T> pagina(String origem, String colunas, UUID filtro, int pagina, int limite, Leitor<T> leitor) {
        String condicao = filtro == null ? "" : " WHERE " + (origem.startsWith("unidades") ? "id" : "pu.unidade_id") + " = ?";
        try (var con = banco.getConnection(); var contar = con.prepareStatement("SELECT count(*) FROM " + origem + condicao);
             var listar = con.prepareStatement("SELECT " + colunas + " FROM " + origem + condicao + " ORDER BY 1 LIMIT ? OFFSET ?")) {
            int indice = 1;
            if (filtro != null) { contar.setObject(1, filtro); listar.setObject(indice++, filtro); }
            listar.setInt(indice++, limite);
            listar.setInt(indice, (pagina - 1) * limite);
            long total;
            try (var rs = contar.executeQuery()) { rs.next(); total = rs.getLong(1); }
            var itens = new ArrayList<T>();
            try (var rs = listar.executeQuery()) { while (rs.next()) itens.add(leitor.ler(rs)); }
            return new Pagina<>(itens, pagina, limite, total);
        } catch (SQLException e) { throw new IllegalStateException("Falha ao consultar catálogo", e); }
    }

    public Pagina<Unidade> unidades(int pagina, int limite) {
        return pagina("unidades", "id,nome,endereco", null, pagina, limite,
                rs -> new Unidade(rs.getObject(1, UUID.class), rs.getString(2), rs.getString(3)));
    }

    public boolean unidadeExiste(UUID id) {
        return existe("SELECT 1 FROM unidades WHERE id=?", id);
    }

    public Optional<Unidade> unidade(UUID id) {
        try (var con = banco.getConnection(); var sql = con.prepareStatement("SELECT id,nome,endereco FROM unidades WHERE id=?")) {
            sql.setObject(1, id);
            try (var rs = sql.executeQuery()) {
                return rs.next() ? Optional.of(new Unidade(rs.getObject(1, UUID.class), rs.getString(2), rs.getString(3))) : Optional.empty();
            }
        } catch (SQLException e) { throw new IllegalStateException("Falha ao consultar unidade", e); }
    }

    private boolean existe(String consulta, UUID id) {
        try (var con = banco.getConnection(); var sql = con.prepareStatement(consulta)) {
            sql.setObject(1, id);
            try (var rs = sql.executeQuery()) { return rs.next(); }
        } catch (SQLException e) { throw new IllegalStateException("Falha ao consultar registro", e); }
    }

    public Pagina<Produto> cardapio(UUID id, int pagina, int limite) {
        String origem = "produtos_unidades pu JOIN produtos p ON p.id=pu.produto_id AND pu.disponivel=true AND pu.saldo_estoque>0";
        return pagina(origem, "p.id,p.nome,p.descricao,pu.preco,pu.disponivel", id, pagina, limite,
                rs -> new Produto(rs.getObject(1, UUID.class), rs.getString(2), rs.getString(3), rs.getBigDecimal(4), rs.getBoolean(5)));
    }

    public Optional<UUID> unidadeDoUsuario(UUID id) {
        try (var con = banco.getConnection(); var sql = con.prepareStatement("SELECT unidade_id FROM usuarios WHERE id=? AND perfil IN ('ATENDENTE','COZINHA')")) {
            sql.setObject(1, id);
            try (var rs = sql.executeQuery()) { return rs.next() ? Optional.ofNullable(rs.getObject(1, UUID.class)) : Optional.empty(); }
        } catch (SQLException e) { throw new IllegalStateException("Falha ao consultar unidade do usuário", e); }
    }

    public Pagina<Saldo> estoques(UUID id, int pagina, int limite) {
        return pagina("produtos_unidades pu JOIN produtos p ON p.id=pu.produto_id", "pu.id,pu.unidade_id,p.id,p.nome,pu.saldo_estoque", id, pagina, limite,
                rs -> new Saldo(rs.getObject(1, UUID.class), rs.getObject(2, UUID.class), rs.getObject(3, UUID.class), rs.getString(4), rs.getInt(5)));
    }

    public Optional<Integer> bloquearSaldo(UUID id) {
        try (var con = banco.getConnection(); var sql = con.prepareStatement("SELECT saldo_estoque FROM produtos_unidades WHERE id=? FOR UPDATE")) {
            sql.setObject(1, id);
            try (var rs = sql.executeQuery()) { return rs.next() ? Optional.of(rs.getInt(1)) : Optional.empty(); }
        } catch (SQLException e) { throw new IllegalStateException("Falha ao bloquear estoque", e); }
    }

    public void atualizarSaldo(UUID id, int saldo) {
        try (var con = banco.getConnection(); var sql = con.prepareStatement("UPDATE produtos_unidades SET saldo_estoque=? WHERE id=?")) {
            sql.setInt(1, saldo); sql.setObject(2, id); sql.executeUpdate();
        } catch (SQLException e) { throw new IllegalStateException("Falha ao atualizar estoque", e); }
    }

    public void inserirMovimento(UUID id, UUID produtoUnidadeId, String tipo, int quantidade) {
        try (var con = banco.getConnection(); var sql = con.prepareStatement("INSERT INTO movimentacoes_estoque (id,produto_unidade_id,tipo,quantidade,origem) VALUES (?,?,?::tipo_movimentacao_estoque,?,'MANUAL')")) {
            sql.setObject(1, id); sql.setObject(2, produtoUnidadeId); sql.setString(3, tipo); sql.setInt(4, quantidade); sql.executeUpdate();
        } catch (SQLException e) { throw new IllegalStateException("Falha ao registrar movimento", e); }
    }
}
