package br.com.raizesnordeste.infrastructure;

import br.com.raizesnordeste.domain.CadastroDuplicado;
import br.com.raizesnordeste.domain.RepositorioUsuarios;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class RepositorioUsuariosPostgres implements RepositorioUsuarios {
    @Inject
    DataSource banco;

    public boolean unidadeExiste(UUID id) {
        try (var con = banco.getConnection(); var sql = con.prepareStatement("SELECT 1 FROM unidades WHERE id=?")) {
            sql.setObject(1, id);
            try (var rs = sql.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Falha ao consultar unidade", e);
        }
    }

    public void inserir(Registro u) {
        try (var con = banco.getConnection();
                var sql = con.prepareStatement(
                        "INSERT INTO usuarios (id,nome,cpf,data_nascimento,email,senha_hash,perfil,unidade_id) VALUES (?,?,?,?,?,?,?::perfil_usuario,?)")) {
            sql.setObject(1, u.id());
            sql.setString(2, u.nome());
            sql.setString(3, u.cpf());
            sql.setObject(4, u.nascimento());
            sql.setString(5, u.email());
            sql.setString(6, u.senhaHash());
            sql.setString(7, u.perfil());
            sql.setObject(8, u.unidadeId());
            sql.executeUpdate();
        } catch (SQLException e) {
            if ("23505".equals(e.getSQLState())) {
                throw new CadastroDuplicado();
            }
            throw new IllegalStateException("Falha ao cadastrar usuário", e);
        }
    }

    public Optional<Registro> buscarPorEmail(String email) {
        try (var con = banco.getConnection();
                var sql = con.prepareStatement("SELECT id, perfil, senha_hash FROM usuarios WHERE lower(email)=?")) {
            sql.setString(1, email);
            try (var rs = sql.executeQuery()) {
                return rs.next() ? Optional.of(new Registro(rs.getObject("id", UUID.class), null, null, null, email,
                        rs.getString("senha_hash"), rs.getString("perfil"), null)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Falha ao consultar usuário", e);
        }
    }

    public Optional<Registro> alterarUnidade(UUID id, UUID unidadeId) {
        try (var con = banco.getConnection();
                var sql = con.prepareStatement(
                        "UPDATE usuarios SET unidade_id=? WHERE id=? AND perfil IN ('ATENDENTE','COZINHA') RETURNING nome,perfil")) {
            sql.setObject(1, unidadeId);
            sql.setObject(2, id);
            try (var rs = sql.executeQuery()) {
                return rs.next()
                        ? Optional.of(new Registro(id, rs.getString("nome"), null, null, null, null,
                                rs.getString("perfil"), unidadeId))
                        : Optional.empty();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Falha ao alterar unidade", e);
        }
    }
}
