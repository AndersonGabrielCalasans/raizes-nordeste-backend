package br.com.raizesnordeste.infrastructure;

import br.com.raizesnordeste.domain.Auditoria.Evento;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class AuditoriaPostgresTest {
    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> tipo, java.lang.reflect.InvocationHandler handler) {
        return (T) Proxy.newProxyInstance(tipo.getClassLoader(), new Class<?>[] { tipo }, handler);
    }

    @Test
    void gravaSomenteOsCamposDoEvento() {
        Map<Integer, Object> parametros = new HashMap<>();
        boolean[] executado = { false };
        var comando = proxy(PreparedStatement.class, (p, metodo, argumentos) -> {
            if (metodo.getName().startsWith("set")) parametros.put((Integer) argumentos[0], argumentos[1]);
            if (metodo.getName().equals("executeUpdate")) { executado[0] = true; return 1; }
            return null;
        });
        var conexao = proxy(Connection.class, (p, metodo, argumentos) -> {
            if (metodo.getName().equals("prepareStatement")) return comando;
            return null;
        });
        var repositorio = new AuditoriaPostgres();
        repositorio.banco = proxy(DataSource.class, (p, metodo, argumentos) -> conexao);
        UUID responsavel = UUID.randomUUID();
        UUID recurso = UUID.randomUUID();
        repositorio.registrar(new Evento(responsavel, "LOGIN", "USUARIO", recurso, "SUCESSO"));
        assertTrue(executado[0]);
        assertInstanceOf(UUID.class, parametros.get(1));
        assertEquals(responsavel, parametros.get(2));
        assertEquals("LOGIN", parametros.get(3));
        assertEquals("USUARIO", parametros.get(4));
        assertEquals(recurso, parametros.get(5));
        assertEquals("SUCESSO", parametros.get(6));
        assertEquals(6, parametros.size());
    }

    @Test
    void informaFalhaDePersistencia() {
        var repositorio = new AuditoriaPostgres();
        repositorio.banco = proxy(DataSource.class, (p, metodo, argumentos) -> { throw new SQLException(); });
        assertThrows(IllegalStateException.class,
                () -> repositorio.registrar(new Evento(null, "LOGIN", "USUARIO", null, "RECUSADO")));
    }
}
