package br.com.raizesnordeste.api;

import br.com.raizesnordeste.domain.Auditoria;
import io.quarkus.security.spi.runtime.AuthenticationFailureEvent;
import io.quarkus.security.spi.runtime.AuthorizationFailureEvent;
import io.quarkus.security.spi.runtime.SecurityEvent;
import io.vertx.ext.web.RoutingContext;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.ObservesAsync;
import jakarta.inject.Inject;
import java.util.UUID;

@ApplicationScoped
public class AuditoriaSeguranca {
    @Inject
    Auditoria auditoria;

    void autenticacaoRecusada(@ObservesAsync AuthenticationFailureEvent evento) {
        registrar(evento, "AUTENTICACAO_RECUSADA");
    }

    void autorizacaoRecusada(@ObservesAsync AuthorizationFailureEvent evento) {
        registrar(evento, "ACESSO_NEGADO");
    }

    private void registrar(SecurityEvent evento, String operacao) {
        var contexto = (RoutingContext) evento.getEventProperties().get(RoutingContext.class.getName());
        if (contexto == null) return;
        String caminho = contexto.request().path();
        String recurso = caminho.equals("/usuarios") || caminho.startsWith("/usuarios/") ? "USUARIO"
                : caminho.equals("/estoque") || caminho.startsWith("/estoque/") ? "ESTOQUE"
                : caminho.equals("/pedidos") || caminho.startsWith("/pedidos/") ? "PEDIDO" : null;
        if (recurso == null) return;
        UUID responsavel = null;
        if (evento.getSecurityIdentity() != null && !evento.getSecurityIdentity().isAnonymous()) {
            try {
                responsavel = UUID.fromString(evento.getSecurityIdentity().getPrincipal().getName());
            } catch (IllegalArgumentException ignored) {
                // Uma identidade não reconhecida não é atribuída a um usuário.
            }
        }
        auditoria.registrar(new Auditoria.Evento(responsavel, operacao, recurso, null, "RECUSADO"));
    }
}
