package br.com.raizesnordeste.infrastructure;

import br.com.raizesnordeste.domain.Auditoria;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.sql.SQLException;
import java.util.UUID;
import javax.sql.DataSource;

@ApplicationScoped
public class AuditoriaPostgres implements Auditoria {
    @Inject
    DataSource banco;

    @Override
    @Transactional
    public void registrar(Evento evento) {
        String comando = "INSERT INTO eventos_auditoria (id,responsavel_id,operacao,recurso,recurso_id,resultado) "
                + "VALUES (?,?,?,?,?,?)";
        try (var conexao = banco.getConnection(); var sql = conexao.prepareStatement(comando)) {
            sql.setObject(1, UUID.randomUUID());
            sql.setObject(2, evento.responsavelId());
            sql.setString(3, evento.operacao());
            sql.setString(4, evento.recurso());
            sql.setObject(5, evento.recursoId());
            sql.setString(6, evento.resultado());
            sql.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Falha ao registrar auditoria", e);
        }
    }
}
