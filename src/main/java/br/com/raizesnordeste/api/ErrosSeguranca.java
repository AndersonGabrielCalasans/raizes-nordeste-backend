package br.com.raizesnordeste.api;

import io.quarkus.security.AuthenticationFailedException;
import io.quarkus.security.ForbiddenException;
import io.quarkus.security.UnauthorizedException;
import io.vertx.ext.web.Router;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;

@ApplicationScoped
public class ErrosSeguranca {
    @ServerExceptionMapper
    public Response naoAutenticado(UnauthorizedException erro) {
        return Response.status(401).type(MediaType.APPLICATION_JSON).header("WWW-Authenticate", "Bearer")
                .entity(new ApiErro("NAO_AUTENTICADO", "Token ausente ou inválido.")).build();
    }

    @ServerExceptionMapper
    public Response proibido(ForbiddenException erro) {
        return Response.status(403).type(MediaType.APPLICATION_JSON)
                .entity(new ApiErro("ACESSO_NEGADO", "Perfil sem permissão.")).build();
    }

    void configurar(@Observes Router router) {
        router.route().failureHandler(contexto -> {
            if (contexto.failure() instanceof AuthenticationFailedException && !contexto.response().ended()) {
                contexto.response().setStatusCode(401).putHeader("Content-Type", "application/json")
                        .putHeader("WWW-Authenticate", "Bearer")
                        .end("{\"codigo\":\"NAO_AUTENTICADO\",\"mensagem\":\"Token ausente ou inválido.\",\"detalhes\":[]}");
            } else contexto.next();
        });
    }
}
